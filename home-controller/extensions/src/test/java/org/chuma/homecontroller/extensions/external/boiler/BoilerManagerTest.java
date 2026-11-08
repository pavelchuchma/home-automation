package org.chuma.homecontroller.extensions.external.boiler;

import junit.framework.TestCase;

import org.chuma.homecontroller.extensions.external.boiler.BoilerManager.Correction;

public class BoilerManagerTest extends TestCase {

    private static State state(boolean on, int targetTemp, boolean eHeat) {
        return new State(State.MODE.unlocked, 0, 40, 35, 20, 15, 30, 25, targetTemp, on, false, eHeat, false, false,
                new State.LastFailure(KeySequence.none, 0, State.MODE.unknown));
    }

    public void testInIntervalMatchingStateNeedsNoCorrection() {
        assertEquals(Correction.NONE, BoilerManager.getCorrection(true, 45, state(true, 45, false)));
    }

    public void testInIntervalOffTurnsOn() {
        assertEquals(Correction.TURN_ON, BoilerManager.getCorrection(true, 45, state(false, 45, false)));
    }

    public void testInIntervalWrongTargetTempTurnsOn() {
        assertEquals(Correction.TURN_ON, BoilerManager.getCorrection(true, 45, state(true, 50, false)));
    }

    public void testOutsideIntervalOnTurnsOff() {
        assertEquals(Correction.TURN_OFF, BoilerManager.getCorrection(false, 45, state(true, 45, false)));
    }

    public void testOutsideIntervalOffIgnoresTargetTemp() {
        assertEquals(Correction.NONE, BoilerManager.getCorrection(false, 45, state(false, 50, false)));
    }

    public void testEHeatIsNotChecked() {
        assertEquals(Correction.NONE, BoilerManager.getCorrection(true, 45, state(true, 45, true)));
        assertEquals(Correction.NONE, BoilerManager.getCorrection(false, 45, state(false, 45, true)));
    }
}
