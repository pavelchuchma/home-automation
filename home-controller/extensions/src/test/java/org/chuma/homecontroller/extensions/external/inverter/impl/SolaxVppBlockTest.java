package org.chuma.homecontroller.extensions.external.inverter.impl;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * Layout of the VPP register blocks. No inverter hardware or network access is involved.
 */
public class SolaxVppBlockTest extends TestCase {
    /**
     * The mode 8 example from the SolaX article: PV off, battery charging 5000 W, 1800 s, leave VPP afterwards.
     */
    public void testSolaxExample() {
        Assert.assertArrayEquals(
                new int[]{0x0008, 0x0001, 0x0000, 0x0000, 0xEC78, 0xFFFF, 0x0708, 0x00A0},
                SolaxInverterModbusClient.buildVppPvBatteryBlock(0, -5000, 1800));
    }

    public void testDischargeWithPvLimitAbove16Bits() {
        Assert.assertArrayEquals(
                new int[]{0x0008, 0x0001, 0x1170, 0x0001, 0x07D0, 0x0000, 0x0078, 0x00A0},
                SolaxInverterModbusClient.buildVppPvBatteryBlock(70000, 2000, 120));
    }

    public void testInvalidArgumentsAreRejected() {
        assertRejected(-1, 0, 120);
        assertRejected(0, 0, 0);
        assertRejected(0, 0, 0x10000);
    }

    private static void assertRejected(int pvLimitW, int batteryW, int durationS) {
        try {
            SolaxInverterModbusClient.buildVppPvBatteryBlock(pvLimitW, batteryW, durationS);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
