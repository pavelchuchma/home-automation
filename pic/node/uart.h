#ifndef UART_H
#define	UART_H

#include "system.h"

typedef volatile struct {
    union {
        char bytes[7];
        struct {
            char nodeId;
            char messageType;
            char data[5];
        };
    };
    unsigned length :7;
    unsigned isUart :1; // 1 = uart, 0 = can
} Packet;

typedef volatile struct {
    char data[9];
    unsigned dataLength :4;
    unsigned possition  :4;
} PacketBuffer;

typedef volatile struct {
    char buff[32];
    char r;
    char w;
    char packetCount;
} RWBuffer;

extern volatile RWBuffer receiveQueue;
extern volatile Packet receivedPacket;
extern volatile Packet outPacket;
extern volatile PacketBuffer sendPacketBuffer;

#define isUartSendBufferFree() (sendPacketBuffer.dataLength == 0)

char uart_sendPacket(Packet *source);
void uart_readPacket(void);
void uart_putReceiveQueue(char c);
char uart_popReceiveQueue(void);

char can_sendPacket(Packet *source);
void can_readPacket(void);

#endif	/* UART_H */

