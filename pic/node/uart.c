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

#include "uart.h"
#include "system.h"

volatile RWBuffer receiveQueue;
volatile Packet receivedPacket;
volatile Packet outPacket;
volatile PacketBuffer sendPacketBuffer;

/**
 * Waits for isUartSendBufferFree() and puts packet to sendPacketBuffer
 * and starts transmission
 * @param source packet to be send
 * @return
 *  - 0 in case of success
 *  - 1 no data in source packet
 */
char uart_sendPacket(Packet *source) {
    while (!isUartSendBufferFree());
    if (!source->length) return 1;

    sendPacketBuffer.possition = 0;
    char crc = source->length;
    char eighthBits = 128;
    char i;

    for (i=0; i<source->length; i++) {
        unsigned char c = source->bytes[i];
        crc += c;
        sendPacketBuffer.data[i] = (c & 127) + 128;
        if (c & 128) {
            eighthBits += (1 << i);
        }
    }
    crc += eighthBits;
    sendPacketBuffer.data[i++] = eighthBits;
    sendPacketBuffer.data[i++] = crc & 127;
    sendPacketBuffer.dataLength = i;
    PIE1bits.TXIE = 1;
    return 0;
}



/**
 * Puts char to receiveQueue
 * Intcrements appFlags.receiveBufferErrCount in case of full queue
 * @param c char to be put
 */
void uart_putReceiveQueue(char c) {
    char nextW = ((receiveQueue.w + 1) & 31);
    // check if full
    if (receiveQueue.r == nextW) {
        appFlags.uartReceiveBufferErrCount++;
        //reset buffer if it is full but no packet was received
        if (!receiveQueue.packetCount) {
            receiveQueue.r = 0;
            receiveQueue.w = 0;
        }
    } else {
        if (!(c & 128)) receiveQueue.packetCount++;
        receiveQueue.buff[receiveQueue.w] = c;
        receiveQueue.w = nextW;
    }
}

/**
 * Pops character from queue
 * @return
 */
char uart_popReceiveQueue() {
    // check if empty
    if (receiveQueue.r == receiveQueue.w) return 0;
    char c = receiveQueue.buff[receiveQueue.r];
    receiveQueue.r = ((receiveQueue.r + 1) & 31);
    return c;
}

/**
 * Reads one packet from 'receiveQueue' to 'receivedPacket'
 * - If succeded, then receivedPacket.length > 0
 * - receivedPacket.length == 0 if there are no data ready
 * - receivedPacket.length == 0 & appFlags.receiveCrcErrCount is incremented in case of error.
 */
void uart_readPacket() {
    receivedPacket.length = 0;
    if (!receiveQueue.packetCount) return;

    char dataLen = 0;
    char i, crcByte, eighthBits;
    for (i=0;i<9;i++) {
        // read char from buffer
        char c = receiveQueue.buff[receiveQueue.r];
        receiveQueue.r = (receiveQueue.r + 1) & 31;
        if (c & 128) {
            //data byte
            receivedPacket.bytes[i] = c;
            eighthBits = c;
        } else {
            dataLen = i - 1;
            crcByte = c;
            break;
        }
    }
    // set length to 0 (can be rewritten by loop above
    receivedPacket.length = 0;
    if (dataLen) {
        // complete packet is read
        receiveQueue.packetCount--;

        char crc = dataLen + eighthBits;
        // compute crc & set 8th bits

        if (!(eighthBits & 1)) receivedPacket.bytes[0] &= 127;
        crc += receivedPacket.bytes[0];
        if (dataLen == 1) goto eighthBitsDone;

        if (!(eighthBits & 2)) receivedPacket.bytes[1] &= 127;
        crc += receivedPacket.bytes[1];
        if (dataLen == 2) goto eighthBitsDone;

        if (!(eighthBits & 4)) receivedPacket.bytes[2] &= 127;
        crc += receivedPacket.bytes[2];
        if (dataLen == 3) goto eighthBitsDone;

        if (!(eighthBits & 8)) receivedPacket.bytes[3] &= 127;
        crc += receivedPacket.bytes[3];
        if (dataLen == 4) goto eighthBitsDone;

        if (!(eighthBits & 16)) receivedPacket.bytes[4] &= 127;
        crc += receivedPacket.bytes[4];
        if (dataLen == 5) goto eighthBitsDone;

        if (!(eighthBits & 32)) receivedPacket.bytes[5] &= 127;
        crc += receivedPacket.bytes[5];
        if (dataLen == 6) goto eighthBitsDone;

        if (!(eighthBits & 64)) receivedPacket.bytes[6] &= 127;
        crc += receivedPacket.bytes[6];

eighthBitsDone:

        // check crc
        if ((crc & 127)  == crcByte) {
            receivedPacket.length = dataLen;
            receivedPacket.isUart = 1;
            return;
        } else {
             //crc failure, report CRC error
            appFlags.uartReceiveCrcErrCount++;
        }
    } else {
        //too long packet, report CRC error
        appFlags.uartReceiveCrcErrCount++;
    }
}

void can_readPacket() {
    if (!COMSTATbits.NOT_FIFOEMPTY) {
        receivedPacket.length = 0;
        return;
    }
    // set EWIN4-0 to 00010ccc, where 'ccc' are 3 lowest bites of (CANCON.FP3-0)
    ECANCON = ECANCON & 0b11100000 | 0b00010000 | (CANCON & 0b00001111);

    if (RXB0DLC & 0b00001111 <= 6) {
        canReceiveLongMsgCount++;
    } else if (!RXB0CONbits.RXFUL) {
        canReceiveMismatch++;
    } else {
        receivedPacket.nodeId = RXB0SIDH;
        receivedPacket.messageType = RXB0D0;
        receivedPacket.data[0] = RXB0D1;
        receivedPacket.data[1] = RXB0D2;
        receivedPacket.data[2] = RXB0D3;
        receivedPacket.data[3] = RXB0D4;
        receivedPacket.data[4] = RXB0D5;

        receivedPacket.length = (RXB0DLC & 0b00000111) + 1;
        receivedPacket.isUart = 0;
    }
    // clear full flag
    RXB0CONbits.RXFUL = 0;
}

char can_sendPacket(Packet *source) {
    if (!source->length) return 1;

    //Select transmit buffer 0
    ECANCON = (ECANCON & 0b11100000) | 0b00000011;

    while (MAPPED_CONbits.MAPPED_TXREQ); // wait for transmitt buffer is ready
    RXB0SIDH = source->nodeId;
    RXB0SIDL = 0;
    RXB0D0 = source->messageType;
    RXB0D1 = source->data[0];
    RXB0D2 = source->data[1];
    RXB0D3 = source->data[2];
    RXB0D4 = source->data[3];
    RXB0D5 = source->data[4];
    RXB0DLC = source->length - 1;

    MAPPED_CONbits.MAPPED_TXREQ = 1; // send message
    return 0;
}
