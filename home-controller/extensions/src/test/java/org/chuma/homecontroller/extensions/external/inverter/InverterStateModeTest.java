package org.chuma.homecontroller.extensions.external.inverter;

import junit.framework.TestCase;

import org.chuma.homecontroller.extensions.external.inverter.InverterState.Mode;

/**
 * Mapping of the Solax RunMode register to {@link Mode}. No inverter hardware is involved.
 */
public class InverterStateModeTest extends TestCase {
    public void testCodesFromTheSpec() {
        assertEquals(Mode.Waiting, Mode.fromCode(0));
        assertEquals(Mode.Checking, Mode.fromCode(1));
        assertEquals(Mode.Normal, Mode.fromCode(2));
        assertEquals(Mode.Fault, Mode.fromCode(3));
        assertEquals(Mode.PermanentFault, Mode.fromCode(4));
        assertEquals(Mode.Updating, Mode.fromCode(5));
        assertEquals(Mode.EPSCheck, Mode.fromCode(6));
        assertEquals(Mode.EPSMode, Mode.fromCode(7));
        assertEquals(Mode.SelfTest, Mode.fromCode(8));
        assertEquals(Mode.Idle, Mode.fromCode(9));
        assertEquals(Mode.Standby, Mode.fromCode(10));
    }

    public void testRemoteControl() {
        assertEquals(Mode.RemoteControl, Mode.fromCode(20));
    }

    public void testUnknownCodeDoesNotThrow() {
        assertEquals(Mode.Unknown, Mode.fromCode(11));
        assertEquals(Mode.Unknown, Mode.fromCode(65535));
        assertEquals(Mode.Unknown, Mode.fromCode(Integer.MIN_VALUE));
    }

    public void testRunning() {
        for (Mode mode : Mode.values()) {
            assertEquals(mode.name(), mode == Mode.Normal || mode == Mode.RemoteControl, mode.isRunning());
        }
    }
}
