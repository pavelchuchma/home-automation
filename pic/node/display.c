#if defined(__XC)
    #include <xc.h>        /* XC8 General Include File */
#elif defined(HI_TECH_C)
    #include <htc.h>       /* HiTech General Include File */
#elif defined(__18CXX)
    #include <p18cxxx.h>   /* C18 General Include File */
#endif

#include "system.h"

char digitToBCD(char digit) @ 0xE0
{
    WREG = digit * 2;
    asm ("addwf PCL, F");
#if 0
    asm ("retlw 0xD0");
    asm ("retlw 0xD1");
    asm ("retlw 0xD2");
    asm ("retlw 0xD3");
    asm ("retlw 0xD4");
    asm ("retlw 0xD5");
    asm ("retlw 0xD6");
    asm ("retlw 0xD7");
    asm ("retlw 0xD8");
    asm ("retlw 0xD9");
#else
    asm ("retlw 0b00010010");
    asm ("retlw 0b11011011");
    asm ("retlw 0b10000110");
    asm ("retlw 0b10000011");
    asm ("retlw 0b01001011");
    asm ("retlw 0b00100011");
    asm ("retlw 0b00100010");
    asm ("retlw 0b10011011");
    asm ("retlw 0b00000010");
    asm ("retlw 0b00000011");
#endif
    return 0;
}

void recalculateDisplayValue() {
    unsigned short long num = displayValue;

    char tmp = 0; // count 100 000s
    while (num > 99999) {
        num -= 100000;
        tmp++;
    }
    displaySegments[5] = digitToBCD(tmp);

    char tmp = 0; // count 10 000s
    while (num > 9999) {
        num -= 10000;
        tmp++;
    }
    displaySegments[4] = digitToBCD(tmp);

    char tmp = 0; // count 1000s
    while (num > 999) {
        num -= 1000;
        tmp++;
    }
    displaySegments[3] = digitToBCD(tmp);

    char tmp = 0; // count 100s
    while (num > 99) {
        num -= 100;
        tmp++;
    }
    displaySegments[2] = digitToBCD(tmp);

    tmp = 0; // count 10s
    while (num > 9) {
        num -= 10;
        tmp++;
    }
    displaySegments[1] = digitToBCD(tmp);
    displaySegments[0] = digitToBCD(num);
}