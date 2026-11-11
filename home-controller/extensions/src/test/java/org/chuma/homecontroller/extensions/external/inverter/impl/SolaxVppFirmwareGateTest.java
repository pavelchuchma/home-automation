package org.chuma.homecontroller.extensions.external.inverter.impl;

import junit.framework.TestCase;

/**
 * The VPP commands on an inverter whose firmware has no VPP. Nothing connects to an inverter: the commands
 * must fail before any write.
 */
public class SolaxVppFirmwareGateTest extends TestCase {
    private static class OldFirmwareClient extends SolaxInverterModbusClient {
        OldFirmwareClient() throws Exception {
            super("127.0.0.1");
        }

        @Override
        public synchronized boolean isVppSupported() {
            return false;
        }
    }

    public void testFirmwareVersion() {
        assertFalse(SolaxInverterModbusClient.isVppFirmware(1, 44));
        assertTrue(SolaxInverterModbusClient.isVppFirmware(1, 45));
        assertTrue(SolaxInverterModbusClient.isVppFirmware(1, 54));
        assertTrue(SolaxInverterModbusClient.isVppFirmware(2, 0));
        assertFalse(SolaxInverterModbusClient.isVppFirmware(0, 99));
    }

    public void testPvBatteryIsRefused() throws Exception {
        try {
            new OldFirmwareClient().vppPvBattery(0, -2000, 120);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not support VPP"));
        }
    }

    public void testDisableIsRefused() throws Exception {
        try {
            new OldFirmwareClient().vppDisable();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not support VPP"));
        }
    }
}
