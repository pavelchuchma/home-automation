/******************************************************************************/
/*Files to Include                                                            */
/******************************************************************************/

#if defined(__XC)
#include <xc.h>         /* XC8 General Include File */
#elif defined(HI_TECH_C)
#include <htc.h>        /* HiTech General Include File */
#elif defined(__18CXX)
#include <p18cxxx.h>    /* C18 General Include File */
#endif

#if defined(__XC) || defined(HI_TECH_C)

#include <stdint.h>         /* For uint8_t definition */
#include <stdbool.h>        /* For true/false definition */

#endif

#include "system.h"
#include "Constants.h"
#include "uart.h"

char nodeId = NODE_ID;

AppFlags appFlags;
volatile PortConfig portConfig = {{0, 0, 0, 0}, {0, 0, 0, 0}};

volatile char heartBeatCounter;
volatile char heartBeatPeriod;
volatile unsigned short long displayValue;
volatile unsigned short long displayValueOld;
volatile char displaySegments[6];

volatile char canReceiveLongMsgCount @300;
volatile char canReceiveMismatch @301;

/* Refer to the device datasheet for information about available
oscillator configurations. */
void configureOscillator(char freqMHz) {
    // Oscilator setup
    if (freqMHz == 16) {
        // todo: Nefunguje!!!, nastavi na 4MHz
        OSCCON = 0b01100010; //IDLEN IRCF2 IRCF1 IRCF0 OSTS IOFS SCS1 SCS0
        OSCTUNE = 0b01000000; //INTSRC PLLEN ? TUN4 TUN3 TUN2 TUN1 TUN0
    } else if (freqMHz == 8) {
        OSCCON = 0b01110010; //IDLEN IRCF2 IRCF1 IRCF0 OSTS IOFS SCS1 SCS0
        OSCTUNE = 0b00000000; //INTSRC PLLEN ? TUN4 TUN3 TUN2 TUN1 TUN0
    } else if (freqMHz == 4) {
        OSCCON = 0b01100010; //IDLEN IRCF2 IRCF1 IRCF0 OSTS IOFS SCS1 SCS0
        OSCTUNE = 0b00000000; //INTSRC PLLEN ? TUN4 TUN3 TUN2 TUN1 TUN0
    } else if (freqMHz == 2) {
        OSCCON = 0b01010010; //IDLEN IRCF2 IRCF1 IRCF0 OSTS IOFS SCS1 SCS0
        OSCTUNE = 0b00000000; //INTSRC PLLEN ? TUN4 TUN3 TUN2 TUN1 TUN0
    } else {
        //set 1 MHz
        OSCCON = 0b01000010; //IDLEN IRCF2 IRCF1 IRCF0 OSTS IOFS SCS1 SCS0
        OSCTUNE = 0b00000000; //INTSRC PLLEN ? TUN4 TUN3 TUN2 TUN1 TUN0
    }

    // wait until new clock source is stable
#if !defined (__DEBUG)
    while (!OSCCONbits.IOFS);
#endif



    /* Typical actions in this function are to tweak the oscillator tuning
    register, select new clock sources, and to wait until new clock sources
    are stable before resuming execution of the main project. */
}

void checkUartErrors() {
    // report receiveBufferErrCount
    if (appFlags.uartReceiveBufferErrCount) {
        outPacket.messageType = MSG_ErrorReport;
        outPacket.data[0] = MSG_ERR_UartReceiveBufferErrorCount;
        outPacket.data[1] = appFlags.uartReceiveBufferErrCount;
        outPacket.data[2] = receiveQueue.r;
        outPacket.data[3] = receiveQueue.w;
        outPacket.data[4] = receiveQueue.packetCount;
        appFlags.uartReceiveBufferErrCount -= outPacket.data[1]; //thread safe clearing
        outPacket.length = 7;

        uart_sendPacket(&outPacket);
    }
    // report receiveCrcErrCount;
    if (appFlags.uartReceiveCrcErrCount) {
        outPacket.messageType = MSG_ErrorReport;
        outPacket.data[0] = MSG_ERR_UartReceiveCrcErrorCount;
        outPacket.data[1] = appFlags.uartReceiveCrcErrCount;
        appFlags.uartReceiveCrcErrCount -= outPacket.data[1]; //thread safe clearing
        outPacket.length = 4;

        uart_sendPacket(&outPacket);
    }
}

void processSetPort() {
    //set tris
    /*    PORTA-D 0xF80-3
     *    TRISA-D 0xF92-5
     */
#define setPortRequest (*(MsgSetPortRequest*) &receivedPacket)
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_SetPortResponse;
    outPacket.data[0] = receivedPacket.messageType;
    outPacket.length = 4;
    FSR1H = 0x0F;
    if (receivedPacket.length > 4) {
        // event mask
        portConfig.eventMask[receivedPacket.messageType - MSG_SetPortA] = setPortRequest.eventMask;
        if (receivedPacket.length == 6) {
            // set tris register
            FSR1L = (char) &TRISA + (receivedPacket.messageType - MSG_SetPortA);
            INDF1 = setPortRequest.trisValue;
            //store tris value to outPacket, extend packet size
            outPacket.data[2] = INDF1;
            outPacket.length = 5;
        }
    }
    FSR1L = (char) &PORTA + (receivedPacket.messageType - MSG_SetPortA);
    INDF1 = (INDF1 & ~setPortRequest.valueMask) | (setPortRequest.value & setPortRequest.valueMask);

    //store tris value to outPacket
    outPacket.data[1] = INDF1;
}

void checkInputChange() {
    outPacket.nodeId = nodeId;
    outPacket.length = 4;

    volatile unsigned char *ports = &PORTA;

    for (char i = 3; i != 255; i--) {
        char change = (ports[i] ^ portConfig.oldValues[i]) & portConfig.eventMask[i];
        if (change) {
            outPacket.data[0] = change;
            portConfig.oldValues[i] = (ports[i] & portConfig.eventMask[i]);
            outPacket.data[1] = portConfig.oldValues[i];
            outPacket.messageType = MSG_OnPortAPinChange + i;
            if (nodeId == NODE_ROUTER) {
                uart_sendPacket(&outPacket);
            } else {
                can_sendPacket(&outPacket);
            }
        }
    }
}
