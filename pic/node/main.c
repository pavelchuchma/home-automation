/******************************************************************************/
/* Files to Include                                                           */
/******************************************************************************/

#if defined(__XC)
    #include <xc.h>        /* XC8 General Include File */
#elif defined(HI_TECH_C)
    #include <htc.h>       /* HiTech General Include File */
#elif defined(__18CXX)
    #include <p18cxxx.h>   /* C18 General Include File */
#endif

#if defined(__XC) || defined(HI_TECH_C)

#include <stdint.h>        /* For uint8_t definition */
#include <stdbool.h>       /* For true/false definition */

#endif

#include "system.h"        /* System funct/params, like osc/peripheral config */
#include "user.h"          /* User funct/params, such as InitApp */
#include "Constants.h"
#include "UnitTest1.h"
#include "uart.h"

/******************************************************************************/
/* User Global Variable Declaration                                           */
/******************************************************************************/

/* i.e. uint8_t <variable_name>; */

/******************************************************************************/
/* Main Program                                                               */

/******************************************************************************/

/** Sends outPacket to destination from where receivedPacked was received
 */
void sendResponse() {
    if (receivedPacket.isUart) {
        uart_sendPacket(&outPacket);
    } else {
        can_sendPacket(&outPacket);
    }
}

void main(void) {
    /* Configure the oscillator for the device */
    configureOscillator(1);

    /* Initialize I/O and Peripherals for application */
    InitApp();

#if(RUN_TESTS)
    testRunAll();
    return;
#else

    // delay initialization to don't start all nodes at one time
    int delayCount = 2000 * (nodeId - 1);
    for (int i = 0; i < delayCount; i++) {
        NOP();
    }

    PIE1bits.RCIE = 1; //enable interrupts after setup

    struct {
        char packetLeft;
        char packetSize;
        char nextByte;
        char waitForFreeBuffer;
    } perfTestData = {0, 0, 0, 0};

    int loopCounter = 0;
    while (1) {
        // invalidate current packet
        receivedPacket.length = 0;
        // an UART packet received
        if (receiveQueue.packetCount) {
            // get packet from queue
            uart_readPacket();
        } else {
            //try to read packet from CAN
            can_readPacket();
        }
        // is there any packet to process?
        if (receivedPacket.length) {
            // is received packet for me?
            if (receivedPacket.nodeId == nodeId) {
                if (receivedPacket.messageType == MSG_SetHeartBeatPeriod) {
                    // set ping period in seconds (multiply by 2 because timer is set to .5s)
                    heartBeatPeriod = receivedPacket.data[0] * 2 * getCpuFrequency();
                } else if (receivedPacket.messageType == MSG_EchoRequest) {
                    // process Echo message
                    outPacket.nodeId = nodeId;
                    outPacket.messageType = MSG_EchoResponse;
                    outPacket.length = receivedPacket.length;
                    for (char j = 2; j < receivedPacket.length; j++) {
                        outPacket.bytes[j] = receivedPacket.bytes[j];
                    }

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_UartTransmitPerfTestRequest) {
                    perfTestData.packetLeft = (*(MsgUartTransmitPerfTestRequest*) & receivedPacket).packetCount;
                    perfTestData.packetSize = (*(MsgUartTransmitPerfTestRequest*) & receivedPacket).packetLength;
                    perfTestData.nextByte = (*(MsgUartTransmitPerfTestRequest*) & receivedPacket).firstByte;
                    perfTestData.waitForFreeBuffer = receivedPacket.data[3];
                    perfTestData.waitForFreeBuffer = (*(MsgUartTransmitPerfTestRequest*) & receivedPacket).waitForFreeOutput;
                } else if (receivedPacket.messageType == MSG_SetUartBaudRate) {
                    // set new UART baud rate
                    SPBRGH1 = receivedPacket.data[0];
                    SPBRG1 = receivedPacket.data[1];
                } else if (receivedPacket.messageType == MSG_ReadRamRequest) {
                    // send byte from required memory position
                    processReadRamRequest();
                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_WriteRamRequest) {
                    // write byte to required memory position
                    processWriteRamRequest();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_GetBuildTimeRequest) {
                    // store build time to outPacket
                    processGetBuildTimeRequest();

                    // send response to proper destination
                    sendResponse();
                } else if ((receivedPacket.messageType & 0b11111100) == MSG_SetPortA) {
                    // set port value, event mask & tris
                    processSetPort();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_EnablePwmRequest) {
                    // enable PWM module, change CPU frequency if necessary
                    processEnablePwmRequest();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_SetPwmValueRequest) {
                    // set new PWM value
                    processSetPwmValue();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_InitializationFinished) {
                    // set new PWM value
                    appFlags.isInitialized = 1;
                    // force to send heart beat immediately
                    appFlags.onPingTimer = 1;

                } else if (receivedPacket.messageType == MSG_OnDebug) {

                } else if (receivedPacket.messageType == MSG_SetFrequencyRequest) {
                    // set change CPU frequency
                    processSetFrequencyRequest();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_SetManualPwmValueRequest) {
                    // set change CPU frequency
                    processSetManualPwmValueRequest();

                    // send response to proper destination
                    sendResponse();
                } else if (receivedPacket.messageType == MSG_ResetRequest) {
                    // set change CPU frequency
                    processResetRequest();
                    // do not need to wait for anything, reset will be faster and wait for message sending
                    // can be risk of deadlock in some cases (or complicated code)
                } else if (receivedPacket.messageType == MSG_ReadProgramRequest) {
                    // set change CPU frequency
                    processReadProgramRequest();

                    // send response to proper destination
                    sendResponse();
                }
            } else if (nodeId == NODE_ROUTER) {
                //message is not for me, but I'm a router
                if (receivedPacket.isUart) {
                    // forward UART message to CAN
                    can_sendPacket(&receivedPacket);
                } else {
                    // forward CAN message to UART
                    uart_sendPacket(&receivedPacket);
                }
                // increment counter to count of forwarded messages
                receivedPacketCount++;
            }
        }

#if 0
        // UartTransmitPerfTest
        if (perfTestData.packetLeft && (!perfTestData.waitForFreeBuffer || isUartSendBufferFree())) {
            outPacket.nodeId = nodeId;
            outPacket.messageType = MSG_UartTransmitPerfTestMessage;
            outPacket.data[0] = perfTestData.packetLeft--;
            for (char x = 1; x < perfTestData.packetSize; x++) outPacket.data[x] = perfTestData.nextByte++;
            outPacket.length = perfTestData.packetSize + 2;
            //TODO: Update for CAN
            uart_sendPacket(&outPacket);
        }
#endif
        if (appFlags.onPingTimer) {
            appFlags.onPingTimer = 0;
            outPacket.nodeId = nodeId;
            outPacket.data[0] = heartBeatCounter;
            if (!appFlags.isInitialized) {
                outPacket.messageType = MSG_OnReboot;
                outPacket.data[1] = RCON;
                outPacket.length = 4;
            } else {
                outPacket.messageType = MSG_OnHeartBeat;
                outPacket.length = 3;
            }

            if (nodeId == NODE_ROUTER) {
                uart_sendPacket(&outPacket);
            } else {
                can_sendPacket(&outPacket);
            }
        }

        if (nodeId == NODE_ROUTER) {
            if (loopCounter == 0) {
                // update receive packet LED indicator
                if (switchBridgeLedOffCounter == 0) {
                    // turn packet LED OFF
                    PORTBbits.RB1 = 1;
                } else {
                    switchBridgeLedOffCounter--;
                }

                if (receivedPacketCount != receivedPacketCountOld) {
                    receivedPacketCountOld = receivedPacketCount;
                    // turn packet LED ON
                    PORTBbits.RB1 = 0;
                    switchBridgeLedOffCounter = 23; // approx 10 ms
                }
            }
        }

        doManualPwm();

        if (loopCounter++ == 3) {
            loopCounter = 0;
            if (checkInput) {
                checkInputChange();
            }
        } else {
            // nops are here to have both branches of this if of the same length. It is necessary for manual PWM.
            asm ("nop");
            asm ("nop");
            asm ("nop");
            asm ("nop");
            asm ("nop");
            asm ("nop");
            asm ("nop");
            asm ("nop");
        }
    }
#endif
}

// put NOP at the end, otherwise MPSIM returns PC overflow
//asm ("org 0xFFFE");
//asm ("nop");
