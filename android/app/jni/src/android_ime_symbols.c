#include "android_ime_symbols.h"

unsigned android_ime_symbol(uint8_t scancode, bool shift) {
    if (scancode >= 30 && scancode <= 39)
        return shift ? (unsigned char)"!@#$%^&*()"[scancode - 30] : 0;
    const char *pair;
    switch (scancode) {
    case 45: pair = "-_"; break;
    case 46: pair = "=+"; break;
    case 47: pair = "[{"; break;
    case 48: pair = "]}"; break;
    case 49: pair = "\\|"; break;
    case 51: pair = ";:"; break;
    case 52: pair = "'\""; break;
    case 53: pair = "`~"; break;
    case 54: pair = ",<"; break;
    case 55: pair = ".>"; break;
    case 56: pair = "/?"; break;
    default: return 0; /* Letters, plain digits and control keys keep their existing path. */
    }
    return (unsigned char)pair[shift ? 1 : 0];
}

