#include "diagnostics/logger.h"
#include <cstdarg>
#include <cstdio>

namespace pyrosense {

LogLevel Logger::current_level_ = LogLevel::DEBUG;

void Logger::set_level(LogLevel level) {
    current_level_ = level;
}

LogLevel Logger::level() {
    return current_level_;
}

void Logger::verbose(const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(LogLevel::VERBOSE, tag, fmt, args);
    va_end(args);
}

void Logger::debug(const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(LogLevel::DEBUG, tag, fmt, args);
    va_end(args);
}

void Logger::info(const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(LogLevel::INFO, tag, fmt, args);
    va_end(args);
}

void Logger::warn(const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(LogLevel::WARN, tag, fmt, args);
    va_end(args);
}

void Logger::error(const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(LogLevel::ERROR, tag, fmt, args);
    va_end(args);
}

void Logger::log(LogLevel level, const char* tag, const char* fmt, va_list args) {
    if (level < current_level_) return;

    // TODO: Replace with ESP_LOG* macros on real ESP32
    // On host, output to stdout
    printf("[%s] [%s] ", level_char(level), tag);
    vprintf(fmt, args);
    printf("\n");
}

const char* Logger::level_char(LogLevel level) {
    switch (level) {
        case LogLevel::VERBOSE: return "V";
        case LogLevel::DEBUG:   return "D";
        case LogLevel::INFO:    return "I";
        case LogLevel::WARN:    return "W";
        case LogLevel::ERROR:   return "E";
        default:                return "?";
    }
}

}  // namespace pyrosense
