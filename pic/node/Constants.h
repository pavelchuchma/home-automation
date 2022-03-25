/* 
 * File:   Constants.h
 * Author: chuma
 *
 * Created on December 25, 2012, 2:41 PM
 */

#ifndef CONSTANTS_H
#define	CONSTANTS_H


enum ErrorCodes {
    ERR_OK = 0,
    ERR_BAD_PARAMS = 1,

};

enum CcpModuleEnum {
    CCP_CCP1 = 1,
    CCP_CCP2 = 2,
    CCP_CCP3 = 4,
    CCP_CCP4 = 8,
    CCP_ECCP1 = 16,
    CCP_ECCP2 = 32,
};

enum PortEnum {
    PORT_A = 0,
    PORT_B = 1,
    PORT_C = 2,
};

enum Commands {
//Begin Commands
    MSG_SetPortResponse = 15, // data = request Command Id, value, tris (if set)
    MSG_SetPortA = 16, //MsgSetPortRequest - order is important - see Packet.createMsgSetPort()
    MSG_SetPortB = 17, //MsgSetPortRequest
    MSG_SetPortC = 18, //MsgSetPortRequest
    MSG_SetPortD = 19, //MsgSetPortRequest
    MSG_OnPortAPinChange = 20, // data[0] = changes & eventMask, data[1] = port & eventMask
    MSG_OnPortBPinChange = 21, // Order of MSG_OnPortXPinChange is important - see Node.packetReceivedImpl()
    MSG_OnPortCPinChange = 22,
    MSG_OnPortDPinChange = 23,
    MSG_None = 32,
    MSG_SetHeartBeatPeriod = 33, // sets ping period in seconds
    MSG_SetUartBaudRate = 34, //SPBRGH, SPBRG
    MSG_OnHeartBeat = 35,
    MSG_ErrorReport = 36,
    MSG_EchoRequest = 37, //data with up to 5 bytes
    MSG_EchoResponse = 38,
    MSG_UartTransmitPerfTestRequest = 39, //MsgUartTransmitPerfTestRequest
    MSG_UartTransmitPerfTestMessage = 40, //packetNumber, byte incremented by 1 from firstByte in first packet
    MSG_ReadRamRequest = 41, //MsgReadRamRequest
    MSG_ReadRamResponse = 42, //1 byte
    MSG_WriteRamRequest = 43, //2 bytes address, mask, value
    MSG_WriteRamResponse = 44, //2 bytes (original and new value)
    MSG_OnDebug = 45, //
    MSG_GetBuildTimeRequest = 47, // no data
    MSG_GetBuildTimeResponse = 48, // 5 bytes: ymdhm
    MSG_EnablePwmRequest = 49, // data: 0-ccpMask (CcpModuleEnum), 1-cpuFreqData, 2-canBaudRatePrescaler, 3-pwm value
    MSG_EnablePwmResponse = 50, //1 byte listing enabled CCP modules
    MSG_SetPwmValueRequest = 51, //1 byte CcpModuleEnum, 1 byte value
    MSG_SetPwmValueResponse = 52, //1 byte CcpModuleEnum, 1 byte set value
    MSG_OnReboot = 53, // 1 byte pingCounter, 1 byte RCON value - PIC asks for initialization
    MSG_InitializationFinished = 54, // sends PC to finish initialization of PIC
    MSG_SetFrequencyRequest = 55,  // 0-cpuFreqData, 1-canBaudRatePrescaler
    MSG_SetFrequencyResponse = 56,
    MSG_SetManualPwmValueRequest = 57, // 0(0:3) port, 0(4:7) pin, 1-value
    MSG_SetManualPwmValueResponse = 58, //0-ErrorCodes (0 - OK, 1 - bad param)
    MSG_ResetRequest = 59,
    MSG_ResetResponse = 60, // no data
    MSG_ReadProgramRequest = 61, // 3 bytes (TBLPTRL, TBLPTRH, TBLPTRU)
    MSG_ReadProgramResponse = 62, // 4 bytes

//End Commands
};

enum ErrorReport {
    MSG_ERR_UartReceiveCrcErrorCount = 0,
    MSG_ERR_UartReceiveBufferErrorCount,
};

typedef struct {
    char nodeId;
    char messageType;
} MessageHeader;

typedef struct {
    MessageHeader header;
    unsigned char packetCount;
    unsigned char packetLength;
    unsigned char firstByte;
    unsigned char waitForFreeOutput;
} MsgUartTransmitPerfTestRequest;

typedef struct {
    MessageHeader header;
    unsigned char addressL;
    unsigned char addressH;
} MsgReadRamRequest;

typedef struct {
    MessageHeader header;
    unsigned char tblptrL;
    unsigned char tblptrH;
    unsigned char tblptrU;
} MsgReadProgramRequest;

typedef struct {
    MessageHeader header;
    unsigned char addressL;
    unsigned char addressH;
    unsigned char bitMask;
    unsigned char value;
} MsgWriteRamRequest;

typedef struct {
    MessageHeader header;
    unsigned char valueMask;
    unsigned char value;
    unsigned char eventMask;
    unsigned char trisValue;
} MsgSetPortRequest;

#endif	/* CONSTANTS_H */

#if (0)  //registryMapping
    receiveQueue_r = receiveQueue+32
    receiveQueue_w = receiveQueue+33
    receiveQueue_packetCount = receiveQueue+34

    appFlags_uartReceiveBufferErrCount=appFlags+1
    appFlags_uartReceiveCrcErrCount=appFlags+2

    sendPacketBuffer_dataLength_possition = sendPacketBuffer+9
    portConfig_oldValueA = portConfig+0
    portConfig_oldValueB = portConfig+1
    portConfig_oldValueC = portConfig+2
    portConfig_oldValueD = portConfig+3
    portConfig_eventMaskA = portConfig+4
    portConfig_eventMaskB = portConfig+5
    portConfig_eventMaskC = portConfig+6
    portConfig_eventMaskD = portConfig+7

#endif



