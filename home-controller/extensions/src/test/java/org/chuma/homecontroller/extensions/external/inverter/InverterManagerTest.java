package org.chuma.homecontroller.extensions.external.inverter;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterRemoteClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterTestBase;

public class InverterManagerTest extends SolaxInverterTestBase {
    static Logger log = LoggerFactory.getLogger(InverterManagerTest.class.getName());

    //https://www.zive.cz/clanky/12-webu-a-sluzeb-pro-meteorologicke-geeky/sc-3-a-169154/default.aspx
    @Test
    public void testDate() throws ParseException, InterruptedException {
        final SolaxInverterRemoteClient client = new SolaxInverterRemoteClient(localUrl, localPassword, remoteUsername, remotePasswordToken, basicConfigPin);

        String s = "6:55-8:025;17:55-19:05";
        final InverterManager manager = new InverterManager(client);
        manager.setHighTariffRanges(s);


        Thread.sleep(1200000);
    }

    @Test
    public void setSOC() {
        final SolaxInverterRemoteClient client = new SolaxInverterRemoteClient(localUrl, localPassword, remoteUsername, remotePasswordToken, basicConfigPin);
        int origMinSocValue = client.getConfiguration().getSelfUseMinimalSoc();

        InverterManager manager = new InverterManager(client);
        final int minSoc = 40;
        final int reserve = 13;

        manager.setMinimalSoc(minSoc);
        manager.setBatteryReserve(reserve);

        try {
            manager.applyMinBatterySoc(true);
            Assert.assertEquals(minSoc, client.getConfiguration().getSelfUseMinimalSoc());

            manager.applyMinBatterySoc(false);
            Assert.assertEquals(minSoc + reserve, client.getConfiguration().getSelfUseMinimalSoc());
        } finally {
            client.setSelfUseMinimalSoc(origMinSocValue);
        }
    }
}