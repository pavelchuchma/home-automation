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
    NODE_ALL = 255,
};

#define NODE_ID NODE_2
#define RUN_TESTS 0

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
} PortConfig;

extern volatile PortConfig portConfig;

extern char nodeId;

extern AppFlags appFlags;
extern volatile char heartBeatCounter;
extern volatile char heartBeatPeriod;
extern volatile unsigned short long displayValue;
extern volatile unsigned short long displayValueOld;
extern volatile char displaySegments[6];

extern volatile char canReceiveLongMsgCount;
extern volatile char canReceiveMismatch;


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