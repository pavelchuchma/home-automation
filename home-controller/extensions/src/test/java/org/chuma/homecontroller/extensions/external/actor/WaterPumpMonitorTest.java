package org.chuma.homecontroller.extensions.external.actor;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;
//import org.chuma.homecontroller.app.servlet.WaterPumpJsonSerializer;

public class WaterPumpMonitorTest {
    @Test
    public void testIsOn() throws Exception {
        WaterPumpMonitor mon = new WaterPumpMonitor();
        Action offAction = mon.getOffAction();
        Action onAction = mon.getOnAction();

        Assert.assertEquals("empty", 0, mon.getRecordCount());
        Assert.assertEquals("isOn()", false, mon.isOn());
        offAction.perform(-1);
        Assert.assertEquals("isOn()", false, mon.isOn());
        Assert.assertEquals("empty", 0, mon.getRecordCount());
        Assert.assertEquals("getRecords", 0, mon.getLastRecords(0).length);
        Assert.assertEquals("getRecords", 0, mon.getLastRecords(10).length);

        onAction.perform(1234);
        Assert.assertEquals("isOn()", true, mon.isOn());
        Assert.assertEquals("empty", 0, mon.getRecordCount());
        offAction.perform(10);
        Assert.assertEquals("isOn()", false, mon.isOn());
        Assert.assertEquals("count", 1, mon.getRecordCount());
        Assert.assertEquals("getRecords", 0, mon.getLastRecords(0).length);
        Assert.assertEquals("getRecords", 1, mon.getLastRecords(1).length);
        Assert.assertEquals("getRecords", 1, mon.getLastRecords(10).length);

        Thread.sleep(500);

        onAction.perform(1234);
        Assert.assertEquals("isOn()", true, mon.isOn());
        Assert.assertEquals("empty", 1, mon.getRecordCount());
        offAction.perform(20);
        Assert.assertEquals("isOn()", false, mon.isOn());
        Assert.assertEquals("count", 2, mon.getRecordCount());
        Assert.assertEquals("getRecords", 0, mon.getLastRecords(0).length);
        Assert.assertEquals("getRecords", 1, mon.getLastRecords(1).length);
        Assert.assertEquals("getRecords", 2, mon.getLastRecords(2).length);
        WaterPumpMonitor.Record[] records = mon.getLastRecords(10);
        Assert.assertEquals("getRecords", 2, records.length);
        Assert.assertEquals("getRecords", 0.010, records[0].duration, 0.001);
        Assert.assertEquals("getRecords", 0.020, records[1].duration, 0.001);

        Assert.assertEquals("getRecordCountInLastHours", 2, mon.getRecordCountInLastHours(1));
        Assert.assertEquals("getRecordCountInLastHours", 0, mon.getRecordCountInLastHours(0));

        Thread.sleep(500);

        onAction.perform(1234);
        offAction.perform(30);
        Assert.assertEquals("getRecordCountInLastHours", 3, mon.getRecordCountInLastHours(1));

        Assert.assertEquals("getRecordCountInLastHours", 0, mon.getRecordCountInLastHours(1.0 / (3600 * 1000)));
        Assert.assertEquals("getRecordCountInLastHours", 1, mon.getRecordCountInLastHours(300.0 / (3600 * 1000)));
        Assert.assertEquals("getRecordCountInLastHours", 2, mon.getRecordCountInLastHours(800.0 / (3600 * 1000)));
        Assert.assertEquals("getRecordCountInLastHours", 3, mon.getRecordCountInLastHours(1300.0 / (3600 * 1000)));

//        String json = WaterPumpJsonSerializer.serialize(mon, 3, 800.0 / (3600 * 1000));


    }

}