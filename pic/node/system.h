#ifndef SYSTEM_H
#define	SYSTEM_H
/******************************************************************************/
/* System Level #define Macros                                                */
/******************************************************************************/


/* Microcontroller MIPs (FCY) */
#define SYS_FREQ        8000000L
#define FCY             SYS_FREQ/4

/******************************************************************************/
/* System Function Prototypes                                                 */
/******************************************************************************/

/* Custom oscillator configuration funtions, reset source evaluation
functions, and other non-peripheral microcontroller initialization functions
go here. */

enum NodeId {
    NODE_ROUTER = 1,
    NODE_2 = 2,
    NODE_3 = 3,
    NODE_4 = 4,
    NODE_5 = 5,
    NODE_6 = 6,
    NODE_7 = 7,
    NODE_8 = 8,
    NODE_9 = 9,
    NODE_10 = 10,
    NODE_11 = 11,
    NODE_12 = 12,
    NODE_13 = 13,
    NODE_14 = 14,
    NODE_15 = 15,
    NODE_16 = 16,
    NODE_17 = 17,
    NODE_18 = 18,
    NODE_19 = 19,
    NODE_ALL = 255,
};

#define NODE_ID NODE_13
#define RUN_TESTS 0
#define PWM_RESOLUTION 16
#define PIN_CHANGE_LOOP_COUNT 37

typedef volatile struct {
    struct {
        unsigned isInitialized  :1;
        unsigned onPingTimer    :1;
        unsigned currentSegment :3;
    };
    char uartReceiveBufferErrCount;
    char uartReceiveCrcErrCount;
    char enabledPwmModules;
} AppFlags;

typedef struct {
    char oldValues[4];
    char eventMask[4];
    char eventCounters[32];
} PortConfig;

typedef struct {
    char mask;
    char data[PWM_RESOLUTION];
} ManualPwmData;

extern volatile PortConfig portConfig;

extern char nodeId;

extern volatile AppFlags appFlags;
extern volatile char heartBeatCounter;
extern volatile char heartBeatPeriod;
extern volatile unsigned short long displayValue;
extern volatile unsigned short long displayValueOld;
extern volatile char displaySegments[6];

extern volatile char canReceiveLongMsgCount;
extern volatile char canReceiveMismatch;

extern volatile ManualPwmData manualPwmPortData[3];

#define MAPPED_CONbits  RXB0CONbits
#define MAPPED_CON	RXB0CON
#define MAPPED_SIDH	RXB0SIDH
#define MAPPED_TXREQ	FILHIT3


void recalculateDisplayValue();
void configureOscillator(char freqMHz); /* Handles clock switching/osc initialization */
void ecanSend();
void checkUartErrors();

void processSetPort();
void checkInputChange();

#endif