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
#include "uart.h"

/******************************************************************************/
/* Interrupt Routines                                                         */
/******************************************************************************/

/* High-priority service */

volatile static unsigned short timerEventPrescaler = 0;

#if defined(__XC) || defined(HI_TECH_C)
void __interrupt(high_priority) high_isr(void)
#elif defined (__18CXX)
#pragma code high_isr=0x08
#pragma interrupt high_isr
void high_isr(void)
#else
#error "Invalid compiler selection for implemented ISR routines"
#endif

{

      /* This code stub shows general interrupt handling.  Note that these
      conditional statements are not handled within 3 seperate if blocks.
      Do not use a seperate if block for each interrupt flag to avoid run
      time errors. */

      /* High Priority interrupt routine code here. */

      /* Determine which flag generated the interrupt */
    if (PIR1bits.RCIF && PIE1bits.RCIE) {
        // UART Receive
        if (RCSTAbits.OERR) {
            // An "overrun error" occurs when the receiver cannot process the character that just came in before the next one arrives.
            // Various devices have different amounts of buffer space to hold received characters. The CPU must service the UART in order
            // to remove characters from the input buffer. If the CPU does not service the UART quickly enough and the buffer becomes full,
            // an Overrun Error will occur, and incoming characters will be lost.

            //TODO: log error somewhere
            appFlags.uartReceiveBufferErrCount++;
            // Clear error, if received
            RCSTAbits.CREN = 0;
            RCSTAbits.CREN = 1;
        }
        uart_putReceiveQueue(RCREG);
    } else if (INTCONbits.TMR0IF) {
        INTCONbits.TMR0IF = 0;
        TMR0 = 34297; // 0.5s @1MHz crystal (250 kHz instruction cycle)
        if (++timerEventPrescaler == heartBeatPeriod) {
            timerEventPrescaler = 0;
            heartBeatCounter++;
            appFlags.onPingTimer = 1;
        }
    } else if (PIR1bits.TXIF && PIE1bits.TXIE) {
        // UART Send
        if (sendPacketBuffer.dataLength) {
            TXREG = (sendPacketBuffer.data[sendPacketBuffer.possition++]);

            if (sendPacketBuffer.possition == sendPacketBuffer.dataLength) {
                // All data are written, cler outPacket, disable interrupt
                sendPacketBuffer.dataLength = 0;
                PIE1bits.TXIE = 0;
            }
        } else {
            // invalid state - no data ready but TXIE enabled
            PIE1bits.TXIE = 0;
        }
    } else {
        /* Unhandled interrupts */
        
    }

}

/* Low-priority interrupt routine */
#if defined(__XC) || defined(HI_TECH_C)
void __interrupt(low_priority) low_isr(void)
#elif defined (__18CXX)
#pragma code low_isr=0x18
#pragma interruptlow low_isr
void low_isr(void)
#else
#error "Invalid compiler selection for implemented ISR routines"
#endif
{

      /* This code stub shows general interrupt handling.  Note that these
      conditional statements are not handled within 3 seperate if blocks.
      Do not use a seperate if block for each interrupt flag to avoid run
      time errors. */

#if 0

      /* Low Priority interrupt routine code here. */

      /* Determine which flag generated the interrupt */
      if(TMR0IF) {
          WREG = 10;
      } else {
          /* Unhandled interrupts */
          WREG = 20;
      }

#endif

}
