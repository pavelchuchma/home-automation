/******************************************************************************/
/* User Level #define Macros                                                  */
/******************************************************************************/

/* Application specific user parameters used in user.c may go here */

/******************************************************************************/
/* User Function Prototypes                                                   */
/******************************************************************************/

/* User level functions prototypes (i.e. InitApp) go here */

void InitApp(void);         /* I/O and Peripheral Initialization */
void setupUart(char highByte, char lowByte);
void setupCanBus(char baudRatePrescaller);
void processReadRamRequest(void);
void processWriteRamRequest(void);
void processGetBuildTimeRequest(void);
void processEnablePwmRequest(void);
void processSetPwmValue(void);
void processSetFrequencyRequest(void);
void processSetManualPwmValueRequest(void);
void doManualPwm(void);
void processResetRequest(void);
void processReadProgramRequest(void);
