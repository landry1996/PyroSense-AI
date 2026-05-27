#include "security/nonce_generator.h"
#include <cstdio>
#include <cassert>
#include <set>
#include <string>

using namespace pyrosense;

static void test_nonce_not_empty() {
    std::string nonce = NonceGenerator::generate();
    assert(!nonce.empty());
    assert(nonce.size() == 24);  // 8+8+8 hex chars
    printf("  PASS: nonce_not_empty (len=%zu)\n", nonce.size());
}

static void test_nonce_uniqueness() {
    std::set<std::string> nonces;
    for (int i = 0; i < 100; i++) {
        nonces.insert(NonceGenerator::generate());
    }
    assert(nonces.size() == 100);
    printf("  PASS: nonce_uniqueness (100 unique)\n");
}

static void test_nonce_hex_format() {
    std::string nonce = NonceGenerator::generate();
    for (char c : nonce) {
        bool valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
        assert(valid);
    }
    printf("  PASS: nonce_hex_format\n");
}

static void test_nonce_freshness_valid() {
    std::string nonce = NonceGenerator::generate();
    bool fresh = NonceGenerator::validate_freshness(nonce, 60);
    assert(fresh);
    printf("  PASS: nonce_freshness_valid\n");
}

static void test_nonce_freshness_invalid_format() {
    bool fresh = NonceGenerator::validate_freshness("short", 60);
    assert(!fresh);
    printf("  PASS: nonce_freshness_invalid_format\n");
}

int main() {
    printf("=== Nonce Generator Tests ===\n");
    test_nonce_not_empty();
    test_nonce_uniqueness();
    test_nonce_hex_format();
    test_nonce_freshness_valid();
    test_nonce_freshness_invalid_format();
    printf("=== All Nonce tests passed ===\n");
    return 0;
}
