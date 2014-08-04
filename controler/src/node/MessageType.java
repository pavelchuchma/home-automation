// Generated by N:\work\HomeAutomation\controler\generateMessageType.bat
 
package node; 
 
public class MessageType {
    public static final byte MSG_SetPortResponse = 15; // data = request Command Id, value, tris (if set)
    public static final byte MSG_SetPortA = 16; //MsgSetPortRequest
    public static final byte MSG_SetPortB = 17; //MsgSetPortRequest
    public static final byte MSG_SetPortC = 18; //MsgSetPortRequest
    public static final byte MSG_SetPortD = 19; //MsgSetPortRequest
    public static final byte MSG_OnPortAPinChange = 20; // data[0] = changes & eventMask, data[1] = port & eventMask
    public static final byte MSG_OnPortBPinChange = 21;
    public static final byte MSG_OnPortCPinChange = 22;
    public static final byte MSG_OnPortDPinChange = 23;
    public static final byte MSG_None = 32;
    public static final byte MSG_SetHeartBeatPeriod = 33; // sets ping period in seconds
    public static final byte MSG_SetUartBaudRate = 34; //SPBRGH, SPBRG
    public static final byte MSG_OnHeartBeat = 35;
    public static final byte MSG_ErrorReport = 36;
    public static final byte MSG_EchoRequest = 37;
    public static final byte MSG_EchoResponse = 38;
    public static final byte MSG_UartTransmitPerfTestRequest = 39; //MsgUartTransmitPerfTestRequest
    public static final byte MSG_UartTransmitPerfTestMessage = 40; //packetNumber, byte incremented by 1 from firstByte in first packet
    public static final byte MSG_ReadRamRequest = 41; //MsgReadRamRequest
    public static final byte MSG_ReadRamResponse = 42; //1 byte
    public static final byte MSG_WriteRamRequest = 43; //MsgWriteRamRequest, response is MSG_ReadRamResponse
    public static final byte MSG_WriteRamResponse = 44; //2 bytes (original and new value)
    public static final byte MSG_OnDebug = 45; //
    public static final byte MSG_GetBuildTimeRequest = 47; // no data
    public static final byte MSG_GetBuildTimeResponse = 48; // 5 bytes: ymdhm
    public static final byte MSG_EnablePwmRequest = 49; // data: 0-ccpMask (CcpModuleEnum), 1-cpuFreqData, 2-canBaudRatePrescaler, 3-pwm value
    public static final byte MSG_EnablePwmResponse = 50; //1 byte listing enabled CCP modules
    public static final byte MSG_SetPwmValueRequest = 51; //1 byte CcpModuleEnum, 1 byte value
    public static final byte MSG_SetPwmValueResponse = 52; //1 byte CcpModuleEnum, 1 byte set value
    public static final byte MSG_OnReboot = 53; // 1 byte pingCounter, 1 byte RCON value - PIC asks for initialization
    public static final byte MSG_InitializationFinished = 54; // sends PC to finish initialization of PIC
    public static final byte MSG_SetFrequencyRequest = 55;  // 0-cpuFreqData, 1-canBaudRatePrescaler
    public static final byte MSG_SetFrequencyResponse = 56;
    public static final byte MSG_SetManualPwmValueRequest = 57; // 0(0:3) port, 0(4:7) pin, 1-value
    public static final byte MSG_SetManualPwmValueResponse = 58; //0-ErrorCodes (0 - OK, 1 - bad param)
    public static final byte MSG_ResetRequest = 59;
    public static final byte MSG_ResetResponse = 60; // no data
    public static final byte MSG_ReadProgramRequest = 61; // 3 bytes (TBLPTRL, TBLPTRH, TBLPTRU)
    public static final byte MSG_ReadProgramResponse = 62; // 4 bytes

    public static String toString(int type) {
        switch (type) {
            case MSG_SetPortResponse: return "MSG_SetPortResponse";
            case MSG_SetPortA: return "MSG_SetPortA";
            case MSG_SetPortB: return "MSG_SetPortB";
            case MSG_SetPortC: return "MSG_SetPortC";
            case MSG_SetPortD: return "MSG_SetPortD";
            case MSG_OnPortAPinChange: return "MSG_OnPortAPinChange";
            case MSG_OnPortBPinChange: return "MSG_OnPortBPinChange";
            case MSG_OnPortCPinChange: return "MSG_OnPortCPinChange";
            case MSG_OnPortDPinChange: return "MSG_OnPortDPinChange";
            case MSG_None: return "MSG_None";
            case MSG_SetHeartBeatPeriod: return "MSG_SetHeartBeatPeriod";
            case MSG_SetUartBaudRate: return "MSG_SetUartBaudRate";
            case MSG_OnHeartBeat: return "MSG_OnHeartBeat";
            case MSG_ErrorReport: return "MSG_ErrorReport";
            case MSG_EchoRequest: return "MSG_EchoRequest";
            case MSG_EchoResponse: return "MSG_EchoResponse";
            case MSG_UartTransmitPerfTestRequest: return "MSG_UartTransmitPerfTestRequest";
            case MSG_UartTransmitPerfTestMessage: return "MSG_UartTransmitPerfTestMessage";
            case MSG_ReadRamRequest: return "MSG_ReadRamRequest";
            case MSG_ReadRamResponse: return "MSG_ReadRamResponse";
            case MSG_WriteRamRequest: return "MSG_WriteRamRequest";
            case MSG_WriteRamResponse: return "MSG_WriteRamResponse";
            case MSG_OnDebug: return "MSG_OnDebug";
            case MSG_GetBuildTimeRequest: return "MSG_GetBuildTimeRequest";
            case MSG_GetBuildTimeResponse: return "MSG_GetBuildTimeResponse";
            case MSG_EnablePwmRequest: return "MSG_EnablePwmRequest";
            case MSG_EnablePwmResponse: return "MSG_EnablePwmResponse";
            case MSG_SetPwmValueRequest: return "MSG_SetPwmValueRequest";
            case MSG_SetPwmValueResponse: return "MSG_SetPwmValueResponse";
            case MSG_OnReboot: return "MSG_OnReboot";
            case MSG_InitializationFinished: return "MSG_InitializationFinished";
            case MSG_SetFrequencyRequest: return "MSG_SetFrequencyRequest";
            case MSG_SetFrequencyResponse: return "MSG_SetFrequencyResponse";
            case MSG_SetManualPwmValueRequest: return "MSG_SetManualPwmValueRequest";
            case MSG_SetManualPwmValueResponse: return "MSG_SetManualPwmValueResponse";
            case MSG_ResetRequest: return "MSG_ResetRequest";
            case MSG_ResetResponse: return "MSG_ResetResponse";
            case MSG_ReadProgramRequest: return "MSG_ReadProgramRequest";
            case MSG_ReadProgramResponse: return "MSG_ReadProgramResponse";
        }
        return "Unknown(" + type + ")";
     }
}
