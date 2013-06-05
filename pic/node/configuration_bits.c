/******************************************************************************/
/* Files to Include                                                           */
/******************************************************************************/

#if defined(__XC)
    #include <xc.h>         /* XC8 General Include File */
#elif defined(HI_TECH_C)
    #include <htc.h>        /* HiTech General Include File */
#elif defined(__18CXX)
    #include <p18cxxx.h>    /* C18 General Include File */
#endif

/******************************************************************************/
/* Configuration Bits                                                         */
/*                                                                            */
/* Refer to 'HI-TECH PICC and PICC18 Toolchains > PICC18 Configuration        */
/* Settings' under Help > Contents in MPLAB X IDE for available PIC18         */
/* Configuration Bit Settings for the correct macros when using the C18       */
/* compiler.  When using the Hi-Tech PICC18 compiler, refer to the compiler   */
/* manual.pdf in the compiler installation doc directory section on           */
/* 'Configuration Fuses'.  The device header file in the HiTech PICC18        */
/* compiler installation directory contains the available macros to be        */
/* embedded.  The XC8 compiler contains documentation on the configuration    */
/* bit macros within the compiler installation /docs folder in a file called  */
/* pic18_chipinfo.html.                                                       */
/*                                                                            */
/* For additional information about what the hardware configurations mean in  */
/* terms of device operation, refer to the device datasheet.                  */
/*                                                                            */
/* General C18/XC8 syntax for configuration macros:                           */
/* #pragma config <Macro Name>=<Setting>, <Macro Name>=<Setting>, ...         */
/*                                                                            */
/* General HiTech PICC18 syntax:                                              */
/* __CONFIG(n,x);                                                             */
/*                                                                            */
/* n is the config word number and x represents the anded macros from the     */
/* device header file in the PICC18 compiler installation include directory.  */
/*                                                                            */
/* A feature of MPLAB X is the 'Generate Source Code to Output' utility in    */
/* the Configuration Bits window.  Under Window > PIC Memory Views >          */
/* Configuration Bits, a user controllable configuration bits window is       */
/* available to Generate Configuration Bits source code which the user can    */
/* paste into this project.                                                   */
/*                                                                            */
/******************************************************************************/

/* Fill in your configuration bits here using the config generator.      */

//#pragma config CONFIG1H = 0x08

#if defined(PIC18F26K80)
//  details: file:///C:/Program%20Files%20%28x86%29/Microchip/xc8/v1.12/docs/chips/18f66k80.html

#pragma config SOSCSEL = DIG
#pragma config XINST = OFF
#pragma config FOSC = INTIO2
#pragma config PLLCFG = OFF
#pragma config IESO  = ON
#pragma config WDTEN = OFF
#pragma config CANMX = PORTB


#elif defined(PIC18F2680)
// details: file:///C:/Program%20Files%20%28x86%29/HI-TECH%20Software/PICC-18/9.80/docs/18f2680.html
#pragma config OSC = IRCIO67
// Set PortB:5 as I/O
#pragma config LVP = OFF
// Disable WDT
#pragma config WDT = OFF

#else
#error Unknown device
#endif