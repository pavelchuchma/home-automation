package org.chuma.homecontroller.extensions.external.garage;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class GarageManagerTest {
    int pulseSummary = 0;
    int expectedPulseSummary = 0;

    class TestGarageManager extends GarageManager {
        public TestGarageManager() {
            super((IOnOffActor)null);
        }

        @Override
        public void doPulse(int count) {
            pulseSummary += count;
        }
    }

    @Test
    public void test() {
        pulseSummary = 0;
        expectedPulseSummary = 0;
        GarageManager gm = new TestGarageManager();

        assertEquals(GarageManager.State.UNKNOWN, gm.state);

        gm.open();
        assetState(gm, GarageManager.State.UNKNOWN, 1);

        gm.onOpenContactPressed();
        assetState(gm, GarageManager.State.OPEN, 0);

        gm.onOpenContactReleased();
        assetState(gm, GarageManager.State.CLOSING, 0);

        gm.close();
        assetState(gm, GarageManager.State.STOPPED, 1);

        gm.close();
        assetState(gm, GarageManager.State.CLOSING, 3);
    }

    private void assetState(GarageManager gm, GarageManager.State expState, int expPulseCount) {
        assertEquals(expState, gm.state);
        int newPulseCount = pulseSummary - expectedPulseSummary;
        assertEquals(expPulseCount, newPulseCount);
        expectedPulseSummary += newPulseCount;
    }
}