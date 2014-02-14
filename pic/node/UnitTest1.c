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

#include "system.h"
#include <assert.h>
#include "UnitTest1.h"
#include "Constants.h"
#include "user.h"
#include "uart.h"

#if(RUN_TESTS)

#define ASSERT_EQUALS(a,b) if ((a) != (b)) while(1)

void testRunAll() {
    testButonEvents();
    //testProcessSetManualPwmValueRequest();
//    testDisplay();
//    testEnablePwm();
//    testSetPortRequest();
//    testCheckInputChange();
    //    testTmp();
//    testWriteRamRequest();
//    testUartSendInterrupt();
//    testReceiveBuffer();
//    testUart01();
}



void testButonEvents() {
    portConfig.eventMask[1] = 0b00101000;
    TRISB = 0;
    PORTB = 0;
    // clear packet
    outPacket.messageType = outPacket.data[0] = outPacket.data[1] = 0xFF;
    checkInputChange();

    PORTB = 0b00101111;
    for (char i=0; i<8; i++) {
        if (i == 5) {
            PORTB = 0b00100111;
            checkInputChange();
            PORTB = 0b00101111;
        }
        checkInputChange();
        ASSERT_EQUALS(outPacket.data[0], 0xFF);
    }
    checkInputChange();
    ASSERT_EQUALS(outPacket.messageType, MSG_OnPortBPinChange);
    ASSERT_EQUALS(outPacket.data[0], 0b00100000);
    ASSERT_EQUALS(outPacket.data[1], 0b00100000);

    // clear packet
    outPacket.messageType = outPacket.data[0] = outPacket.data[1] = 0xFF;

    for (char i=0; i<5; i++) {
        checkInputChange();
        ASSERT_EQUALS(outPacket.data[0], 0xFF);
    }
    checkInputChange();
    ASSERT_EQUALS(outPacket.messageType, MSG_OnPortBPinChange);
    ASSERT_EQUALS(outPacket.data[0], 0b00001000);
    ASSERT_EQUALS(outPacket.data[1], 0b00101000);

    // clear packet
    outPacket.messageType = outPacket.data[0] = outPacket.data[1] = 0xFF;

    for (char i=0; i<50; i++) {
        checkInputChange();
        ASSERT_EQUALS(outPacket.data[0], 0xFF);
    }

    PORTB = 0b00100111;
    for (char i=0; i<9; i++) {
        checkInputChange();
        ASSERT_EQUALS(outPacket.data[0], 0xFF);
    }
    checkInputChange();
    ASSERT_EQUALS(outPacket.messageType, MSG_OnPortBPinChange);
    ASSERT_EQUALS(outPacket.data[0], 0b00001000);
    ASSERT_EQUALS(outPacket.data[1], 0b00100000);
}



void comparePacketBuffer(PacketBuffer *pb, char data[]) {
    char a = data[0];
    ASSERT_EQUALS(pb->dataLength, data[0]);
    ASSERT_EQUALS(pb->possition, 0);

    for (char i = 0; i < data[0]; i++) {
        ASSERT_EQUALS(pb->data[i], data[i + 1]);
    }
}

void comparePacket(Packet *p, char expData[]) {
    char len = expData[7];
    ASSERT_EQUALS(p->length, len);
    for (char i = 0; i < len; i++) {
        ASSERT_EQUALS(p->bytes[i], expData[i]);
    }
}

void putDataToReceiveBuffer(char *c) {
    char len = c[0];
    for (char j = 1; j <= len; j++) {
        uart_putReceiveQueue(c[j]);
    }
}

void testUart01() {
    char packets[][sizeof(Packet)] = {
            {10,200,127,128,255,100,100,5},
            {25,33,100,5,5,5,5,3},
            {25,33,100,101,130,255,125,7},
        };

    char expectedPBs[][11] = { //length + 9 bytes of ddata
        {7, 138, 200, 255, 128, 255, 154, 111, 0, 0, 0},// #0
        {5,153,161,228,128,33,0,0,0,0,0},               // #1
        {9,153,161,228,229,130,255,253,176,56,0},       // #2
        {5,153,161,228,128,44,0,0,0,0,0},               // #3 bad CRC
        {10,153,161,228,128,144,223,135,145,222,100},   // #4 too long, bad CRC
    };

    // put 1st 4 packets to receivedPacketBuffer
    putDataToReceiveBuffer(expectedPBs[0]);
    putDataToReceiveBuffer(expectedPBs[3]);
    putDataToReceiveBuffer(expectedPBs[1]);
    putDataToReceiveBuffer(expectedPBs[4]);

    appFlags.uartReceiveCrcErrCount = 0;
    // read and verify all packets
    // #0 - OK
    uart_readPacket();
    comparePacket(&receivedPacket, &packets[0]);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 0);

    // #3 - bad CRC
    uart_readPacket();
    ASSERT_EQUALS(receivedPacket.length, 0);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 1);

    // #1 - ok
    uart_readPacket();
    comparePacket(&receivedPacket, &packets[1]);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 1);

    // #4 - too long, bad CRC
    uart_readPacket();
    ASSERT_EQUALS(receivedPacket.length, 0);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 2);

    // #4 - there should be one byte in buffer (crc from previous - extra long packet)
    uart_readPacket();
    ASSERT_EQUALS(receivedPacket.length, 0);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 3);
    ASSERT_EQUALS(receiveQueue.r, receiveQueue.w); //buffer should be empty

    // put the last packet to buffer
    putDataToReceiveBuffer(expectedPBs[2]);

    // #2 - ok
    uart_readPacket();
    comparePacket(&receivedPacket, &packets[2]);
    ASSERT_EQUALS(appFlags.uartReceiveCrcErrCount, 3);
    ASSERT_EQUALS(receiveQueue.r, receiveQueue.w); //buffer should be empty


    //test uartSend
    for (char i = 0; i<sizeof (packets) / sizeof (Packet); i++) {
        sendPacketBuffer.dataLength = 0; // invalidate outPacket;
        uart_sendPacket((Packet*) packets[i]);
        comparePacketBuffer(&sendPacketBuffer, &expectedPBs[i]);
    }
}

void testReceiveBuffer() {
    //tests receiveBuffer_get() and receiveBuffer_get() functions
    ASSERT_EQUALS(appFlags.uartReceiveBufferErrCount, 0);

    for (char i = 10; i < 50; i++) {
        uart_putReceiveQueue(i);
    }

    ASSERT_EQUALS(appFlags.uartReceiveBufferErrCount, 9);
    char c = uart_popReceiveQueue();
    ASSERT_EQUALS(c, 10);
    uart_putReceiveQueue(50);
    c = uart_popReceiveQueue();
    ASSERT_EQUALS(c, 11);

    for (char j = 0; j < 29; j++) {
        c = uart_popReceiveQueue();
        ASSERT_EQUALS(c, j + 12);
    }
    c = uart_popReceiveQueue();
    ASSERT_EQUALS(c, 50);
    // should be empty
    ASSERT_EQUALS(receiveQueue.r, receiveQueue.w);
    c = uart_popReceiveQueue();
    ASSERT_EQUALS(c, 0);
}

void testUartSendInterrupt() {
    //allow interrupts to test uart sending
    char INTCONbckp = INTCON;
    INTCON = 0b11000000;    //GIE/GIEH PEIE/GIEL TMR0IE INT0IE RBIE TMR0IF INT0IF RBIF
    // send 1 byte long packet
    sendPacketBuffer.data[0] = 'a';
    sendPacketBuffer.dataLength = 1;
    sendPacketBuffer.possition = 0;
    PIE1bits.TXIE = 1;

    // wait for finishing
    while (sendPacketBuffer.dataLength);
    ASSERT_EQUALS(PIE1bits.TXIE, 0);

    //send 9 bytes long packet
    sendPacketBuffer.data[0] = 'A';
    sendPacketBuffer.data[1] = 'B';
    sendPacketBuffer.data[2] = 'C';
    sendPacketBuffer.data[3] = 'D';
    sendPacketBuffer.data[4] = 'E';
    sendPacketBuffer.data[5] = 'F';
    sendPacketBuffer.data[6] = 'G';
    sendPacketBuffer.data[7] = 'H';
    sendPacketBuffer.data[8] = 'I';
    sendPacketBuffer.dataLength = 9;
    sendPacketBuffer.possition = 0;
    PIE1bits.TXIE = 1;

    // wait for finishing
    while (sendPacketBuffer.dataLength);
    ASSERT_EQUALS(PIE1bits.TXIE, 0);

    //restore INTCON
    INTCON = INTCONbckp;

}

void testTmp() {
//    char a[200];
//    a[10] = 0xFE;
//
//    char h = 0x0F;
//    char l = 0xFA;
//
//    FSR1H = h;
//    FSR1L = l;
//    char z = INDF1;
}

void testWriteRamRequest() {
    char tmp[200];
    tmp[10] = 123;

    *((short*)&(*(MsgWriteRamRequest*)&receivedPacket).addressL) = (short)tmp+10;
    receivedPacket.nodeId = NODE_ID;
    receivedPacket.messageType = MSG_WriteRamRequest;
    receivedPacket.length = 6;
    (*(MsgWriteRamRequest*)&receivedPacket).bitMask = 0xFF;
    (*(MsgWriteRamRequest*)&receivedPacket).value = 35;

    processWriteRamRequest();
    ASSERT_EQUALS(tmp[10], 35);
    ASSERT_EQUALS(outPacket.length, 4);
    ASSERT_EQUALS(outPacket.data[0], 123);
    ASSERT_EQUALS(outPacket.data[1], 35);

    tmp[10] = 0b10110001;
    (*(MsgWriteRamRequest*)&receivedPacket).bitMask = 0b00111100;
    (*(MsgWriteRamRequest*)&receivedPacket).value = 0b00101000;
    processWriteRamRequest();
    ASSERT_EQUALS(tmp[10], 0b10101001);
    ASSERT_EQUALS(outPacket.length, 4);
    ASSERT_EQUALS(outPacket.data[0], 0b10110001);
    ASSERT_EQUALS(outPacket.data[1], 0b10101001);
}

void testSetPortRequest() {
    TRISB = 0x00;
    PORTB = 0;

#define setPortRequest (*(MsgSetPortRequest*) &receivedPacket)
    receivedPacket.messageType = MSG_SetPortB;
    receivedPacket.data[0] = 0b11110001; //valueMask
    receivedPacket.data[1] = 0b00111101; //value
    receivedPacket.data[2] = 0b00000011; //eventMask
    receivedPacket.data[3] = 0b10000011; //tris
    receivedPacket.length = 6;

    processSetPort();
    ASSERT_EQUALS(TRISB, 0b10000011);
    ASSERT_EQUALS(PORTB, 0b00110000);
    ASSERT_EQUALS(portConfig.eventMask[1], 0b00000011);

    TRISB = 0x00;
    PORTB = 0;
    portConfig.eventMask[1] = 35;
    receivedPacket.length = 4;
    processSetPort();
    ASSERT_EQUALS(PORTB, 0b00110001);
    ASSERT_EQUALS(portConfig.eventMask[1], 35);
    ASSERT_EQUALS(TRISB, 0b00000000);
}

void clearOutPacket() {
    outPacket.messageType = outPacket.nodeId = outPacket.data[0] = outPacket.data[1] = outPacket.data[2] = outPacket.data[3] = outPacket.length = 0;
}

void testCheckInputChange() {
    clearOutPacket();
    TRISA = 0;
    portConfig.oldValues[0] = 0b00111011;
    PORTA =                   0b11110101;
    portConfig.eventMask[0] = 0b01010110;
    portConfig.eventMask[1] = portConfig.eventMask[2] = portConfig.eventMask[3] = 0;
    clearOutPacket();
    checkInputChange();

    ASSERT_EQUALS(portConfig.oldValues[0], 0b01010100);
    ASSERT_EQUALS(outPacket.length, 4);
    ASSERT_EQUALS(outPacket.nodeId, nodeId);
    ASSERT_EQUALS(outPacket.messageType, MSG_OnPortAPinChange);
    ASSERT_EQUALS(outPacket.data[0], 0b01000110);
    ASSERT_EQUALS(outPacket.data[1], 0b01010100);

    clearOutPacket();
    checkInputChange();
    ASSERT_EQUALS(outPacket.messageType, 0); //no message sent
    
    TRISB = 0;
    portConfig.oldValues[1] = 0b00001110;
    PORTB =                   0b11110101;
    portConfig.eventMask[1] = 0b00001111;
    clearOutPacket();
    checkInputChange();

    ASSERT_EQUALS(portConfig.oldValues[1], 0b11110101 & 0b00001111);
    ASSERT_EQUALS(outPacket.length, 4);
    ASSERT_EQUALS(outPacket.nodeId, nodeId);
    ASSERT_EQUALS(outPacket.messageType, MSG_OnPortBPinChange);
    ASSERT_EQUALS(outPacket.data[0], 0b00001011);
    ASSERT_EQUALS(outPacket.data[1], 0b11110101 & 0b00001111);

    clearOutPacket();
}

void testEnablePwm() {

    volatile char a;
    char i = 0;

    for (i = 0; i != 255; i++) {
        TMR2;
    }

    receivedPacket.messageType = MSG_EnablePwmRequest;
    receivedPacket.data[0] = CCP_CCP1;
    receivedPacket.data[1] = 4; //freq
    receivedPacket.data[2] = 15; // can BPR
    receivedPacket.data[3] = 14; // pwm value
    receivedPacket.length = 6;

    processEnablePwmRequest();

    // verify frequency
    ASSERT_EQUALS(OSCTUNE, 0);
    ASSERT_EQUALS(OSCCON & 0b01110010, 0b01100010);

    // verify can
    ASSERT_EQUALS(BRGCON1, 0b01000000 + 15);


    ASSERT_EQUALS(CCP1CONbits.DC1B0, (14 & 0b01) != 0);
    ASSERT_EQUALS(CCP1CONbits.DC1B1, (14 & 0b10) != 0);
    ASSERT_EQUALS(CCPR1L, 14>>2);


    //set pwm value to 5
    receivedPacket.messageType = MSG_SetPwmValueRequest;
    receivedPacket.data[0] = CCP_CCP1;
    receivedPacket.data[1] = 7;
    receivedPacket.length = 4;

    processSetPwmValue();

    ASSERT_EQUALS(CCP1CONbits.DC1B0, 1);
    ASSERT_EQUALS(CCP1CONbits.DC1B1, 1);
    ASSERT_EQUALS(CCPR1L, 1);

    //set pwm value to 123
    receivedPacket.messageType = MSG_SetPwmValueRequest;
    receivedPacket.data[0] = CCP_CCP1;
    receivedPacket.data[1] = 54; // 110110
    receivedPacket.length = 4;

    processSetPwmValue();

    ASSERT_EQUALS(CCP1CONbits.DC1B0, 0);
    ASSERT_EQUALS(CCP1CONbits.DC1B1, 1);
    ASSERT_EQUALS(CCPR1L, 0b1101);
}

void freshenDisplay();

void testDisplay() {
    nodeId = NODE_ROUTER;
    InitApp();

    appFlags.currentSegment = 0;
    displayValue = 123;

    //segment 1 - should contain 3
    recalculateDisplayValue();
    freshenDisplay();

    ASSERT_EQUALS(PORTBbits.RB0, 1);
    ASSERT_EQUALS(PORTC & 0x0F, 0b1000);
    ASSERT_EQUALS(PORTA & 0x0F, 0b0011);
    ASSERT_EQUALS(appFlags.currentSegment, 1);

    //segment 1 - should contain 2
    recalculateDisplayValue();
    freshenDisplay();
    ASSERT_EQUALS(PORTBbits.RB0, 0);
    ASSERT_EQUALS(PORTCbits.RC5, 1);
    ASSERT_EQUALS(PORTC & 0x0F, 0b1000);
    ASSERT_EQUALS(PORTA & 0x0F, 0b0110);
    ASSERT_EQUALS(appFlags.currentSegment, 2);

    //segment 2 - should contain 1
    recalculateDisplayValue();
    freshenDisplay();
    ASSERT_EQUALS(PORTBbits.RB0, 0);
    ASSERT_EQUALS(PORTCbits.RC5, 0);
    ASSERT_EQUALS(PORTCbits.RC4, 1);
    ASSERT_EQUALS(PORTAbits.RA5, 0);
    ASSERT_EQUALS(PORTC & 0x0F, 0b1101);
    ASSERT_EQUALS(PORTA & 0x0F, 0b1011);
    ASSERT_EQUALS(appFlags.currentSegment, 3);

    //segment 3 - should contain 0
    recalculateDisplayValue();
    freshenDisplay();
    ASSERT_EQUALS(PORTAbits.RA5, 1);
    ASSERT_EQUALS(PORTCbits.RC5, 0);
    ASSERT_EQUALS(PORTC & 0x0F, 0b0001);
    ASSERT_EQUALS(PORTA & 0x0F, 0b0010);
    ASSERT_EQUALS(appFlags.currentSegment, 4);

    //segment 4 - should contain 0
    recalculateDisplayValue();
    freshenDisplay();
    ASSERT_EQUALS(appFlags.currentSegment, 5);

    //segment 5 - should contain 0
    recalculateDisplayValue();
    freshenDisplay();
    ASSERT_EQUALS(appFlags.currentSegment, 0);


}

void testProcessSetManualPwmValueRequest() {
    receivedPacket.messageType = MSG_SetManualPwmValueRequest;
    TRISC = 0xFF;
    // C4 to 7
    receivedPacket.data[0] = 0x42; //pin 4 on potc (2)
    receivedPacket.data[1] = 7;
    receivedPacket.length = 4;

    processSetManualPwmValueRequest();
    ASSERT_EQUALS(manualPwmPortData[2].mask, 1 << 4);
    char expectedTris = ~(1 << 4);
    ASSERT_EQUALS(TRISC, expectedTris);

    // C3 to 15
    receivedPacket.data[0] = 0x32; //pin 3 on potc (2)
    receivedPacket.data[1] = 15;

    processSetManualPwmValueRequest();
    ASSERT_EQUALS(manualPwmPortData[2].mask, (1 << 4) + (1 << 3));
    char expectedTris = ~((1 << 4) + (1 << 3));
    ASSERT_EQUALS(TRISC, expectedTris);

    // C0 to 4
    receivedPacket.data[0] = 0x02; //pin 0 on potc (2)
    receivedPacket.data[1] = 4;

    processSetManualPwmValueRequest();
    ASSERT_EQUALS(manualPwmPortData[2].mask, (1 << 4) + (1 << 3) + 1);
    char expectedTris = ~((1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(TRISC, expectedTris);

    ASSERT_EQUALS(manualPwmPortData[2].data[0], (1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(manualPwmPortData[2].data[3], (1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(manualPwmPortData[2].data[4], (1 << 4) + (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[6], (1 << 4) + (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[7], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[14], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[15], 0);

    // C4 to 5
    receivedPacket.data[0] = 0x42; //pin 4 on potc (2)
    receivedPacket.data[1] = 5;

    processSetManualPwmValueRequest();
    ASSERT_EQUALS(manualPwmPortData[2].mask, (1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(manualPwmPortData[2].data[0], (1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(manualPwmPortData[2].data[3], (1 << 4) + (1 << 3) + 1);
    ASSERT_EQUALS(manualPwmPortData[2].data[4], (1 << 4) + (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[5], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[6], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[7], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[14], (1 << 3));
    ASSERT_EQUALS(manualPwmPortData[2].data[15], 0);

    return;
}

#endif