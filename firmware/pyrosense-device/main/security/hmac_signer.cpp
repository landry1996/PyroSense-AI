#include "security/hmac_signer.h"
#include "diagnostics/logger.h"
#include <cstring>

#ifndef PYRO_HOST_BUILD
// ESP-IDF mbedtls is available on target
#include <mbedtls/md.h>
#else
// ============================================================================
// Self-contained SHA-256 and HMAC-SHA256 for host builds (no external deps)
// ============================================================================
#include <cstdint>
#include <cstring>

namespace {

struct Sha256Ctx {
    uint32_t state[8];
    uint64_t bitcount;
    uint8_t buffer[64];
    uint32_t buflen;
};

static const uint32_t K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
    0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
    0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
    0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

static inline uint32_t rotr(uint32_t x, uint32_t n) {
    return (x >> n) | (x << (32 - n));
}

static inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (~x & z);
}

static inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

static inline uint32_t sigma0(uint32_t x) {
    return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22);
}

static inline uint32_t sigma1(uint32_t x) {
    return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25);
}

static inline uint32_t gamma0(uint32_t x) {
    return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3);
}

static inline uint32_t gamma1(uint32_t x) {
    return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10);
}

static void sha256_init(Sha256Ctx* ctx) {
    ctx->state[0] = 0x6a09e667;
    ctx->state[1] = 0xbb67ae85;
    ctx->state[2] = 0x3c6ef372;
    ctx->state[3] = 0xa54ff53a;
    ctx->state[4] = 0x510e527f;
    ctx->state[5] = 0x9b05688c;
    ctx->state[6] = 0x1f83d9ab;
    ctx->state[7] = 0x5be0cd19;
    ctx->bitcount = 0;
    ctx->buflen = 0;
}

static void sha256_transform(Sha256Ctx* ctx, const uint8_t block[64]) {
    uint32_t W[64];
    for (int i = 0; i < 16; i++) {
        W[i] = (static_cast<uint32_t>(block[i * 4]) << 24) |
                (static_cast<uint32_t>(block[i * 4 + 1]) << 16) |
                (static_cast<uint32_t>(block[i * 4 + 2]) << 8) |
                (static_cast<uint32_t>(block[i * 4 + 3]));
    }
    for (int i = 16; i < 64; i++) {
        W[i] = gamma1(W[i - 2]) + W[i - 7] + gamma0(W[i - 15]) + W[i - 16];
    }

    uint32_t a = ctx->state[0];
    uint32_t b = ctx->state[1];
    uint32_t c = ctx->state[2];
    uint32_t d = ctx->state[3];
    uint32_t e = ctx->state[4];
    uint32_t f = ctx->state[5];
    uint32_t g = ctx->state[6];
    uint32_t h = ctx->state[7];

    for (int i = 0; i < 64; i++) {
        uint32_t t1 = h + sigma1(e) + ch(e, f, g) + K[i] + W[i];
        uint32_t t2 = sigma0(a) + maj(a, b, c);
        h = g;
        g = f;
        f = e;
        e = d + t1;
        d = c;
        c = b;
        b = a;
        a = t1 + t2;
    }

    ctx->state[0] += a;
    ctx->state[1] += b;
    ctx->state[2] += c;
    ctx->state[3] += d;
    ctx->state[4] += e;
    ctx->state[5] += f;
    ctx->state[6] += g;
    ctx->state[7] += h;
}

static void sha256_update(Sha256Ctx* ctx, const uint8_t* data, size_t len) {
    ctx->bitcount += static_cast<uint64_t>(len) * 8;
    while (len > 0) {
        uint32_t space = 64 - ctx->buflen;
        uint32_t to_copy = (len < space) ? static_cast<uint32_t>(len) : space;
        std::memcpy(ctx->buffer + ctx->buflen, data, to_copy);
        ctx->buflen += to_copy;
        data += to_copy;
        len -= to_copy;
        if (ctx->buflen == 64) {
            sha256_transform(ctx, ctx->buffer);
            ctx->buflen = 0;
        }
    }
}

static void sha256_final(Sha256Ctx* ctx, uint8_t hash[32]) {
    // Padding
    ctx->buffer[ctx->buflen++] = 0x80;
    if (ctx->buflen > 56) {
        while (ctx->buflen < 64) {
            ctx->buffer[ctx->buflen++] = 0x00;
        }
        sha256_transform(ctx, ctx->buffer);
        ctx->buflen = 0;
    }
    while (ctx->buflen < 56) {
        ctx->buffer[ctx->buflen++] = 0x00;
    }
    // Append bit length (big-endian 64-bit)
    for (int i = 7; i >= 0; i--) {
        ctx->buffer[ctx->buflen++] = static_cast<uint8_t>(ctx->bitcount >> (i * 8));
    }
    sha256_transform(ctx, ctx->buffer);

    // Output hash (big-endian)
    for (int i = 0; i < 8; i++) {
        hash[i * 4]     = static_cast<uint8_t>(ctx->state[i] >> 24);
        hash[i * 4 + 1] = static_cast<uint8_t>(ctx->state[i] >> 16);
        hash[i * 4 + 2] = static_cast<uint8_t>(ctx->state[i] >> 8);
        hash[i * 4 + 3] = static_cast<uint8_t>(ctx->state[i]);
    }
}

// Compute SHA-256 of data in one shot
static void sha256(const uint8_t* data, size_t len, uint8_t hash[32]) {
    Sha256Ctx ctx;
    sha256_init(&ctx);
    sha256_update(&ctx, data, len);
    sha256_final(&ctx, hash);
}

// HMAC-SHA256: H((K XOR opad) || H((K XOR ipad) || message))
static void hmac_sha256(const uint8_t* key, size_t key_len,
                        const uint8_t* msg, size_t msg_len,
                        uint8_t output[32]) {
    uint8_t k_padded[64];
    std::memset(k_padded, 0, 64);

    if (key_len > 64) {
        // Keys longer than block size are hashed first
        sha256(key, key_len, k_padded);
    } else {
        std::memcpy(k_padded, key, key_len);
    }

    // Inner hash: H((K XOR ipad) || message)
    uint8_t i_key_pad[64];
    for (int i = 0; i < 64; i++) {
        i_key_pad[i] = k_padded[i] ^ 0x36;
    }

    Sha256Ctx ctx;
    sha256_init(&ctx);
    sha256_update(&ctx, i_key_pad, 64);
    sha256_update(&ctx, msg, msg_len);
    uint8_t inner_hash[32];
    sha256_final(&ctx, inner_hash);

    // Outer hash: H((K XOR opad) || inner_hash)
    uint8_t o_key_pad[64];
    for (int i = 0; i < 64; i++) {
        o_key_pad[i] = k_padded[i] ^ 0x5c;
    }

    sha256_init(&ctx);
    sha256_update(&ctx, o_key_pad, 64);
    sha256_update(&ctx, inner_hash, 32);
    sha256_final(&ctx, output);
}

}  // anonymous namespace
#endif  // PYRO_HOST_BUILD

namespace pyrosense {

HmacSigner::HmacSigner() : enabled_(false) {}

bool HmacSigner::init(const std::string& key_hex) {
    if (key_hex.empty() || key_hex.size() < 64) {
        enabled_ = false;
        Logger::info("SEC", "HMAC disabled (no valid key)");
        return true;
    }

    key_ = hex_to_bytes(key_hex);
    if (key_.size() != 32) {
        Logger::error("SEC", "HMAC key must be 32 bytes (64 hex chars)");
        return false;
    }

    enabled_ = true;
    Logger::info("SEC", "HMAC-SHA256 enabled (key loaded)");
    return true;
}

std::string HmacSigner::sign(const std::string& payload) {
    if (!enabled_) {
        return "disabled";
    }

    uint8_t output[32];

#ifndef PYRO_HOST_BUILD
    // ESP-IDF mbedtls implementation
    mbedtls_md_context_t ctx;
    mbedtls_md_init(&ctx);
    const mbedtls_md_info_t* md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    mbedtls_md_setup(&ctx, md_info, 1);
    mbedtls_md_hmac_starts(&ctx, key_.data(), key_.size());
    mbedtls_md_hmac_update(&ctx,
        reinterpret_cast<const unsigned char*>(payload.data()),
        payload.size());
    mbedtls_md_hmac_finish(&ctx, output);
    mbedtls_md_free(&ctx);
#else
    // Host build: self-contained HMAC-SHA256
    hmac_sha256(key_.data(), key_.size(),
                reinterpret_cast<const uint8_t*>(payload.data()),
                payload.size(),
                output);
#endif

    return bytes_to_hex(output, 32);
}

bool HmacSigner::verify(const std::string& payload, const std::string& signature) {
    if (!enabled_) return true;

    std::string expected = sign(payload);

    // Constant-time comparison to prevent timing attacks
    if (expected.size() != signature.size()) {
        return false;
    }

    volatile uint8_t diff = 0;
    for (size_t i = 0; i < expected.size(); i++) {
        diff |= static_cast<uint8_t>(expected[i]) ^ static_cast<uint8_t>(signature[i]);
    }
    return diff == 0;
}

bool HmacSigner::is_enabled() const {
    return enabled_;
}

std::vector<uint8_t> HmacSigner::hex_to_bytes(const std::string& hex) {
    std::vector<uint8_t> bytes;
    bytes.reserve(hex.size() / 2);
    for (size_t i = 0; i + 1 < hex.size(); i += 2) {
        uint8_t byte = 0;
        for (int j = 0; j < 2; j++) {
            char c = hex[i + j];
            byte <<= 4;
            if (c >= '0' && c <= '9') byte |= (c - '0');
            else if (c >= 'a' && c <= 'f') byte |= (c - 'a' + 10);
            else if (c >= 'A' && c <= 'F') byte |= (c - 'A' + 10);
        }
        bytes.push_back(byte);
    }
    return bytes;
}

std::string HmacSigner::bytes_to_hex(const uint8_t* data, size_t len) {
    static const char hex_chars[] = "0123456789abcdef";
    std::string result;
    result.reserve(len * 2);
    for (size_t i = 0; i < len; i++) {
        result += hex_chars[(data[i] >> 4) & 0x0F];
        result += hex_chars[data[i] & 0x0F];
    }
    return result;
}

}  // namespace pyrosense
