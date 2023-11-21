package org.chuma.homecontroller.extensions.external.futura;

import java.net.InetAddress;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FuturaControllerTest extends AbstractFuturaTestBase {
    static Logger log = LoggerFactory.getLogger(FuturaControllerTest.class.getName());

    public void testGetState() throws Exception {
        log.debug("create");
        FuturaController fc = new FuturaController(futuraIpAddress);
        log.debug("getState");
        long startTime = System.currentTimeMillis();
        State state = fc.getState();
        long duration = System.currentTimeMillis() - startTime;
        log.debug("getState done in {} ms", duration);

        printState(state);
    }

    private static void printState(State state) {
        log.debug("*** STATE ***");
        log.debug("getVentilationSpeed: {}", state.getVentilationSpeed());
        log.debug("getAirTempAmbient: {}", state.getAirTempAmbient());
        log.debug("getAirTempFresh: {}", state.getAirTempFresh());
        log.debug("getAirTempIndoor: {}", state.getAirTempIndoor());
        log.debug("getAirTempWaste: {}", state.getAirTempWaste());
        log.debug("getFilterWearLevelPercent: {}%", state.getFilterWearLevelPercent());
        log.debug("getPowerConsumption: {}W", state.getPowerConsumption());
        log.debug("getHeatRecovering: {}W", state.getHeatRecovering());
        log.debug("getWallControllerCO2: {}", state.getWallControllerCO2());
        log.debug("getWallControllerTemperature: {}C", state.getWallControllerTemperature());
        log.debug("getTimeProgramActive: {}", state.getTimeProgramActive());
    }

    public void testReadInputRegistry() throws Exception {
        InetAddress addr = InetAddress.getByName("192.168.68.159");
        //2. Open the connection
        TCPMasterConnection con = new TCPMasterConnection(addr);
        con.setPort(502);
        con.connect();
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(0, 20);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
//        for (int a = 0; a < 4; a++) {
        log.debug("setUnitID");
        req.setUnitID(1);
        log.debug("setRequest");
        trans.setRequest(req);
        log.debug("execute");
        trans.execute();
        log.debug("getResponse");
        ReadInputRegistersResponse res = (ReadInputRegistersResponse)trans.getResponse();
        for (int n = 0; n < res.getWordCount(); n++) {
            System.out.println("Word " + n + "=" + res.getRegisterValue(n));
        }
        log.debug("print done");
        con.close();
    }

    public void testReadHoldingRegistry() throws Exception {
        InetAddress addr = InetAddress.getByName(futuraIpAddress);
        //2. Open the connection
        TCPMasterConnection con = new TCPMasterConnection(addr);
        con.setPort(502);
        con.connect();
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(0, 1);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
//        for (int a = 0; a < 4; a++) {
        req.setUnitID(1);
        trans.setRequest(req);
        trans.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse)trans.getResponse();
        for (int n = 0; n < res.getWordCount(); n++) {
            System.out.println("Word " + 1 + "=" + res.getRegisterValue(n));
        }
//        }
        //6. Close the connection
        con.close();

    }

    public void testWriteHoldingRegistry() throws Exception {
        InetAddress addr = InetAddress.getByName(futuraIpAddress);
        //2. Open the connection
        TCPMasterConnection con = new TCPMasterConnection(addr);
        con.setPort(502);
        con.connect();
        Register r = new SimpleRegister(1);
        WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(0, new Register[]{r});
        ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
//        for (int a = 0; a < 4; a++) {
        req.setUnitID(1);
        trans.setRequest(req);
        trans.execute();
        WriteMultipleRegistersResponse res = (WriteMultipleRegistersResponse)trans.getResponse();
//        for (int n = 0; n < res.getWordCount(); n++) {
//            System.out.println("Word " + 1 + "=" + res.getRegisterValue(n));
//        }
//        }
        //6. Close the connection
        con.close();
    }
}