package org.chuma.homecontroller.extensions.external.inverter;

import java.net.UnknownHostException;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.AbstractSolaxInverterTestBase;

public class InverterManagerTest extends AbstractSolaxInverterTestBase {
    static Logger log = LoggerFactory.getLogger(InverterManagerTest.class.getName());

    //https://www.zive.cz/clanky/12-webu-a-sluzeb-pro-meteorologicke-geeky/sc-3-a-169154/default.aspx
    public void testDate() throws InterruptedException, UnknownHostException {
        final SolaxInverterModbusClient client = new SolaxInverterModbusClient(localIp);

        String s = "6:55-8:25;17:55-19:05";
        final InverterManager manager = new InverterManager(client);
        manager.setHighTariffRanges(s);

        Thread.sleep(1200000);
    }

    public void testSetSOC() throws UnknownHostException {
        final SolaxInverterModbusClient client = new SolaxInverterModbusClient(localIp);
        int origMinSocValue = client.getState().getSelfUseMinimalSoc();

        InverterManager manager = new InverterManager(client);
        final int minSoc = 40;
        final int reserve = 13;

        manager.setMinimalSoc(minSoc);
        manager.setBatteryReserve(reserve);

        try {
            manager.applyMinBatterySoc(true);
            Assert.assertEquals(minSoc, client.getState().getSelfUseMinimalSoc());

            manager.applyMinBatterySoc(false);
            Assert.assertEquals(minSoc + reserve, client.getState().getSelfUseMinimalSoc());
        } finally {
            client.setSelfUseMinimalSoc(origMinSocValue);
        }
    }
}