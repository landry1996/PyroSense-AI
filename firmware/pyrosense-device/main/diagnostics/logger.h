#pragma once

#include <cstdio>

namespace pyrosense {

enum class LogLevel : uint8_t {
    VERBOSE = 0,
    DEBUG = 1,
    INFO = 2,
    WARN = 3,
    ERROR = 4,
    NONE = 5,
};

class Logger {
public:
    static void set_level(LogLevel level);
    static LogLevel level();

    static void verbose(const char* tag, const char* fmt, ...);
    static void debug(const char* tag, const char* fmt, ...);
    static void info(const char* tag, const char* fmt, ...);
    static void warn(const char* tag, const char* fmt, ...);
    static void error(const char* tag, const char* fmt, ...);

private:
    static LogLevel current_level_;
    static void log(LogLevel level, const char* tag, const char* fmt, va_list args);
    static const char* level_char(LogLevel level);
};

}  // namespace pyrosense
