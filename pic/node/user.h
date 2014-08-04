/******************************************************************************/
/* User Level #define Macros                                                  */
/******************************************************************************/

/* Application specific user parameters used in user.c may go here */

/******************************************************************************/
/* User Function Prototypes                                                   */
/******************************************************************************/

/* User level functions prototypes (i.e. InitApp) go here */

void InitApp();         /* I/O and Peripheral Initialization */
void setupUart(char highByte, char lowByte);
void setupCanBus(char baudRatePrescaller);
void processReadRamRequest();
void processWriteRamRequest();
void processGetBuildTimeRequest();
void processEnablePwmRequest();
void processSetPwmValue();
void processSetFrequencyRequest();
void processSetManualPwmValueRequest();
void doManualPwm();
void processResetRequest();
void processReadProgramRequest();
