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

#if defined(__XC) || defined(HI_TECH_C)

#include <stdint.h>         /* For uint8_t definition */
#include <stdbool.h>        /* For true/false definition */

#endif

#include "user.h"
#include "system.h"
#include "Constants.h"
#include "uart.h"

/******************************************************************************/
/* User Functions                                                             */
/******************************************************************************/

/* <Initialize variables in user.h and insert code for user algorithms.> */

void InitApp(void) {
    // init receiveQueue
    receiveQueue.r = receiveQueue.w = receiveQueue.packetCount = 0;

    // init appFlags;
    *((char*) &appFlags) = 0;
    appFlags.uartReceiveBufferErrCount = appFlags.uartReceiveCrcErrCount = 0;
    appFlags.enabledPwmModules = 0;

    sendPacketBuffer.dataLength = sendPacketBuffer.possition = 0;

    // clear can receive error counters
    canReceiveLongMsgCount = canReceiveMismatch = 0;

    // setup timer, Prescale value = 4
    T0CON = 0b10010001; // TMR0ON T08BIT T0CS T0SE PSA T0PS2 T0PS1 T0PS0
    TMR0 = 0xFF - 1000;

    // setup IO ports
    ANCON0 = 0x00;
    ANCON1 = 0x00;

    ADCON1 = 0b00001111; //? ? VCFG1 VCFG0 PCFG3 PCFG2 PCFG1 PCFG0 to set all ports as I/O ports
    TRISA = 0xFF; // All input
    TRISB = 0xFF; // All input
    TRISC = 0xFF; // All input
    PORTA = 0x00;
    PORTB = 0x00;
    PORTC = 0x00;

    // setup USART if NODE_ROUTER
    if (nodeId == NODE_ROUTER) {
        // setup TRIS for display
        TRISA &= 0b00010000;
        TRISB0 = 0;
        TRISC &= 0b11000000;

        // 832 (0x0340) for 300 Bauds @ 1 MHz clock (250 kHz clock cycle)
        // 12 (0x000C) for 19230 Bauds @ 1 MHz clock (250 kHz clock cycle)
        setupUart(0x00, 0x0C);
    }

    WDTCONbits.SWDTEN = 0; // disable watchdog

    INTCON = 0b11100000; //GIE/GIEH PEIE/GIEL TMR0IE INT0IE RBIE TMR0IF INT0IF RBIF
    INTCON2 = 0b11111111; //RBPU INTEDG0 INTEDG1 INTEDG2 ? TMR0IP ? RBIP
    PIE1 = 0b00000000; //PSPIE ADIE RCIE TXIE SSPIE CCP1IE TMR2IE TMR1IE
    IPR1 = 0b11111111; //PSPIP ADIP RCIP TXIP SSPIP CCP1IP TMR2IP TMR1IP

    setupCanBus(0);


    // init flags
    appFlags.isInitialized = 0;
    appFlags.onPingTimer = 1; // to send onReboot message immediately
    heartBeatCounter = 0;
    heartBeatPeriod = 20; // 10s
    displayValue = 0;
    displayValueOld = 255;


    // init manualPwm
    for (char i = 0; i < 3; i++) {
        manualPwmPortData[i].mask = 0;
        for (char j = PWM_RESOLUTION; j;) {
            manualPwmPortData[i].data[--j] = 0;
        }
    }

    /* Initialize User Ports/Peripherals/Project here */

    /* Setup analog functionality and port direction */

    /* Initialize peripherals */

    /* Configure the IPEN bit (1=on) in RCON to turn on/off int priorities */

    /* Enable interrupts */
#if(RUN_TESTS)
    // diable interrupts if running tests
    INTCON = 0;
#endif
}

void setupUart(char highByte, char lowByte) {
    TRISC7 = 1; //RX
    TRISC6 = 0; //TX

    TXSTA = 0b00100100; //CSRC TX9 TXEN SYNC SENDB BRGH TRMT TX9D
    RCSTA = 0b10010000; //SPEN RX9 SREN CREN ADDEN FERR OERR RX9D
    BAUDCON1 = 0b00001000; //ABDOVF RCIDL ? SCKP BRG16 ? WUE ABDEN
    // 832 (0x0340) for 300 Bauds @ 1 MHz clock (250 kHz clock cycle)
    // 12 (0x000C) for 19230 Bauds @ 1 MHz clock (250 kHz clock cycle)
    SPBRGH1 = highByte;
    SPBRG1 = lowByte;
}


void switchCanBusToConfigMode() {
    CANCON = 0b10000000;
    while (!(CANSTAT & 0b10000000));
}

void setupCanBus(char baudRatePrescaller) {
    TRISB3 = 1; //CAN RX
    TRISB2 = 0; //CAN TX

    // switch to configuration mode and wait for propagation
    switchCanBusToConfigMode();

    //MDSEL = Enhanced FIFO mode (Mode 2)
    ECANCON = 0b10110000; //MDSEL1 MDSEL0 FIFOWM EWIN4 EWIN3 EWIN2 EWIN1 EWIN0

    // set all can buffers as FIFO receive buffers
    BSEL0 = 0b00000000; //B5TXEN B4TXEN B3TXEN B2TXEN B1TXEN B0TXEN ? ?

    // Set CAN to 62.5 kHz (using internal 1Mhz oscilator)
    // Synchronized Jump Width = 1 (2 x TQ)
    // Baud Rate Prescaler = 0 (1 TQ)
    BRGCON1 = 0b01000000; //SJW1 SJW0 BRP5 BRP4 BRP3 BRP2 BRP1 BRP0
    BRGCON1 |= (baudRatePrescaller & 0b00111111);

    // Phase Segment 2 Time Select bit = 0 (PH2 = MAX(PH1, 2))
    // Sample of the CAN bus Line = 1 (3x sample)
    // Phase Segment = 3 TQ
    // Propagation Time Select = 0 (1 TQ)
    BRGCON2 = 0b01010000; //SEG2PHTS SAM SEG1PH2 SEG1PH1 SEG1PH0 PRSEG2 PRSEG1 PRSEG0

    //CIOCON<ENDRHI> = 1 (CANTX pin will drive to Vdd level when CANTX is in recessive state)
    CIOCON = 0b00100000; // ? ? ENDRHI CANCAP ? ? ? ?

    /** Receive */
    // Clear RECEIVE ACCEPTANCE MASK0 to accept all
    RXM0SIDH = 0x00000000; //SID10 SID9 SID8 SID7 SID6 SID5 SID4 SID3
    RXM0SIDL = 0x00000000; //SID2 SID1 SID0 ? EXIDEN(1) ? EID17 EID16

    /** set filter0 */
    RXF0SIDH = 0x00000000; //SID10 SID9 SID8 SID7 SID6 SID5 SID4 SID3
    // Filter will only accept standard ID messages (EXIDEN = 0)
    RXF0SIDL = 0x00000000; //SID2 SID1 SID0 ? EXIDEN ? EID17 EID16

    // Assciate Filter0 to RXB0
    // 0000 = Filter n is associated with RXB0
    RXFBCON0 = 0x00000000; //F1BP_3 F1BP_2 F1BP_1 F1BP_0 F0BP_3 F0BP_2 F0BP_1 F0BP_0

    // Assign mask0 for filter0
    // 00 = Acceptance Mask 0
    MSEL0 = 0x00000000; //FIL3_1 FIL3_0 FIL2_1 FIL2_0 FIL1_1 FIL1_0 FIL0_1 FIL0_0

    // enable filter0
    RXFCON0 = 0x00000001; //RXF7EN RXF6EN RXF5EN RXF4EN RXF3EN RXF2EN RXF1EN RXF0EN
    RXFCON1 = 0x00000000; //RXF15EN RXF14EN RXF13EN RXF12EN RXF11EN RXF10EN RXF9EN RXF8EN

    //    switch to LOOPBACK mode and wait for propagation
    //    CANCON = 0b01000000;
    // switch to NORMAL mode and wait for propagation
    CANCON = 0b00000000;

    //TODO: set CAN acceptance mask to myId & all
}

void processReadRamRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_ReadRamResponse;
    outPacket.length = 3;

    FSR1L = (*(MsgReadRamRequest*) & receivedPacket).addressL;
    FSR1H = (*(MsgReadRamRequest*) & receivedPacket).addressH;
    outPacket.data[0] = INDF1;
}

void processWriteRamRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_WriteRamResponse;
    outPacket.length = 4;

    FSR1H = (*(MsgWriteRamRequest*) & receivedPacket).addressH;
    FSR1L = (*(MsgWriteRamRequest*) & receivedPacket).addressL;
    outPacket.data[0] = INDF1;
    INDF1 &= ~(*(MsgWriteRamRequest*) & receivedPacket).bitMask;
    INDF1 |= ((*(MsgWriteRamRequest*) & receivedPacket).bitMask & (*(MsgWriteRamRequest*) & receivedPacket).value);
    outPacket.data[1] = INDF1;
}

void processGetBuildTimeRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_GetBuildTimeResponse;
    outPacket.length = 7;

    //Year
    outPacket.data[0] = (__DATE__[9] - '0') * 10 + __DATE__[10] - '0';

    //Month
    char tmp1 = __DATE__[0];
    char tmp2 = __DATE__[1];
    char tmp3 = __DATE__[2];

    if (tmp1 == 'M') outPacket.data[1] = 3;
    if (tmp1 == 'M' && tmp3 == 'y') outPacket.data[1] = 5;
    if (tmp1 == 'J') outPacket.data[1] = 6;
    if (tmp1 == 'J' && tmp2 == 'a') outPacket.data[1] = 1;
    if (tmp1 == 'J' && tmp3 == 'l') outPacket.data[1] = 7;
    if (tmp1 == 'A') outPacket.data[1] = 4;
    if (tmp1 == 'A' && tmp2 == 'u') outPacket.data[1] = 8;
    if (tmp1 == 'F') outPacket.data[1] = 2;
    if (tmp1 == 'S') outPacket.data[1] = 9;
    if (tmp1 == 'O') outPacket.data[1] = 10;
    if (tmp1 == 'N') outPacket.data[1] = 11;
    if (tmp1 == 'D') outPacket.data[1] = 12;

    //Day
    outPacket.data[2] = (__DATE__[4] - '0') * 10 + __DATE__[5] - '0';

    //Hour
    outPacket.data[3] = (__TIME__[0] - '0') * 10 + __TIME__[1] - '0';
    //Min
    outPacket.data[4] = (__TIME__[3] - '0') * 10 + __TIME__[4] - '0';
}

void setCCP1PwmValue(char value) {
    // set tris of CCP1
    TRISC2 = 0;
    // set duty cycle and enable PWM
    CCPR1L = value >> 2;
    CCP1CON = (CCP1CON & 0b00001111) | ((value & 0b00000011) << 4);

    outPacket.data[1] = ((CCP1CON >> 4) & 0x11) | (CCPR1L << 2);
}

void processEnablePwmRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_EnablePwmResponse;
    outPacket.length = 3;


    // not initialized yet
    if (!appFlags.enabledPwmModules) {
        // setup timer, Prescale value = 32
        T0CON = 0b10010100; // TMR0ON T08BIT T0CS T0SE PSA T0PS2 T0PS1 T0PS0

        // change CPU frequency
        configureOscillator(receivedPacket.data[1]);
        setupCanBus(receivedPacket.data[2]);

        // PWM Period = (PR2) + 1] * 4 * TOSC
        PR2 = 15;
        // start timer
        TMR2 = 0;
        T2CON = 0b00000100; // ? T2OUTPS3 T2OUTPS2 T2OUTPS1 T2OUTPS0 TMR2ON T2CKPS1 T2CKPS0
    }

    // Enable PWN on CCP1 if required
    if (receivedPacket.data[0] == CCP_CCP1) {
        appFlags.enabledPwmModules |= CCP_CCP1;

        // set tris of CCP1
        TRISC2 = 0;
        // set duty cycle and enable PWM
        CCPR1L = 0;
        CCP1CON = 0b00001111; //? ? DC1B1 DC1B0 CCP1M3 CCP1M2 CCP1M1 CCP1M0

        setCCP1PwmValue(receivedPacket.data[3]);
    }

    outPacket.data[0] = appFlags.enabledPwmModules;
}

void processSetPwmValue() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_SetPwmValueResponse;
    outPacket.length = 4;


    // is desired module enabled?
    if ((appFlags.enabledPwmModules & receivedPacket.data[0]) != receivedPacket.data[0]) {
        // it is not enabled, exit
        outPacket.data[0] = outPacket.data[1] = 0;
        return;
    }

    if (receivedPacket.data[0] == CCP_CCP1) {
        setCCP1PwmValue(receivedPacket.data[1]);
    }

    outPacket.data[0] = appFlags.enabledPwmModules;
}

void processSetFrequencyRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_SetFrequencyResponse;
    outPacket.length = 2;

    // disable CAN module to don't do a mess on wire while changing frequency
    switchCanBusToConfigMode();
    // change CPU frequency
    configureOscillator(receivedPacket.data[0]);
    setupCanBus(receivedPacket.data[1]);
}

void processResetRequest() {
    // do reset
    asm ("RESET");
}

void processReadProgramRequest() {
    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_ReadProgramResponse;

    if (receivedPacket.length != 5) {
        // incorrect request length -> do nothing
        outPacket.length = 2;
        return;
    }
    TBLPTRL = (*(MsgReadProgramRequest*) & receivedPacket).tblptrL;
    TBLPTRH = (*(MsgReadProgramRequest*) & receivedPacket).tblptrH;
    TBLPTRU = (*(MsgReadProgramRequest*) & receivedPacket).tblptrU;

    asm ("TBLRD*+");
    outPacket.data[0] = TABLAT;
    asm ("TBLRD*+");
    outPacket.data[1] = TABLAT;
    asm ("TBLRD*+");
    outPacket.data[2] = TABLAT;
    asm ("TBLRD*+");
    outPacket.data[3] = TABLAT;

    outPacket.length = 6;
}

void processSetManualPwmValueRequest() {

    outPacket.nodeId = nodeId;
    outPacket.messageType = MSG_SetManualPwmValueResponse;
    outPacket.data[0] = ERR_OK;
    outPacket.length = 3;

    char portIndex = receivedPacket.data[0] & 0x0F;

    if (portIndex > 2) {  //verify port
        outPacket.data[0] = ERR_BAD_PARAMS;
        return;
    }


    char pin = receivedPacket.data[0] >> 4;
    if (receivedPacket.data[1] > PWM_RESOLUTION) receivedPacket.data[1] = PWM_RESOLUTION;

    volatile ManualPwmData *pwmData = manualPwmPortData + portIndex;

    char mask = 1 << pin;
    pwmData->mask |= mask;

    // set all TRIS bits corresponding to the mask to output (0)
    *(&TRISA + portIndex) &= ~pwmData->mask;

    // set '1' to data from 0 to 'value-1'
    for (char i = 0; i < receivedPacket.data[1]; i++) {
        pwmData->data[i] |= mask;
    }
    // set '0' to data from 'value' to PWM_RESOLUTION-1
    // invert mask
    mask ^= 0xFF;
    for (char i = receivedPacket.data[1]; i < PWM_RESOLUTION; i++) {
        pwmData->data[i] &= mask;
    }
}

#define SET_PORT_MANUAL_PWM(port, index) \
    if (manualPwmPortData[index].mask) {    \
        port |= (manualPwmPortData[index].mask & manualPwmPortData[index].data[position]); /*set ones */ \
        port &= ((~manualPwmPortData[index].mask) | manualPwmPortData[index].data[position]); /* set zeros */ \
    }


void doManualPwm() {
    static char position = 0;

    SET_PORT_MANUAL_PWM(PORTA, 0);
    SET_PORT_MANUAL_PWM(PORTB, 1);
    SET_PORT_MANUAL_PWM(PORTC, 2);

    position++;
    if (position == PWM_RESOLUTION) position = 0;
}