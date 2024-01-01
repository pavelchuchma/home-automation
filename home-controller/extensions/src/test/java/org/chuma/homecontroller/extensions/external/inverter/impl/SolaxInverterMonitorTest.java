package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitorTest;

public class SolaxInverterMonitorTest extends AbstractSolaxInverterTestBase {
    public void testMonitor() throws Exception {
        SolaxInverterMonitor monitor = new SolaxInverterMonitor(
                new SolaxInverterModbusClient(localIp), 3_000, 8_000);

        AbstractStateMonitorTest.processMonitorStateTest(monitor, 1000);
    }

    public void testGetBeforeStart() throws Exception {
        SolaxInverterMonitor monitor = new SolaxInverterMonitor(
                new SolaxInverterModbusClient(localIp), 3_000, 8_000);

        AbstractStateMonitorTest.processGetBeforeStartTest(monitor, 1000);
    }
}