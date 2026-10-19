package org.chuma.homecontroller.app.servlet.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.futura.FuturaController;

public class FuturaHandlerTest {
    /**
     * Records register writes instead of sending them, nothing connects to the unit.
     */
    private static class RecordingController extends FuturaController {
        final List<String> writes = new ArrayList<>();

        RecordingController() throws Exception {
            super("127.0.0.1");
        }

        @Override
        protected void writeRegister(int address, int value) {
            writes.add(address + "=" + value);
        }
    }

    private static Map<String, String[]> params(String... keyValues) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], new String[]{keyValues[i + 1]});
        }
        return map;
    }

    @Test
    public void onlyGivenParamsAreWritten() throws Exception {
        RecordingController controller = new RecordingController();

        FuturaHandler.applyAction(controller, params("id", "futura", "bypass", "false"));

        Assert.assertEquals(List.of("14=0"), controller.writes);
    }

    @Test
    public void allParams() throws Exception {
        RecordingController controller = new RecordingController();

        FuturaHandler.applyAction(controller, params("id", "futura", "vent", "3", "timeProg", "true", "tempSet", "21.5",
                "humiSet", "75", "bypass", "true"));

        Assert.assertEquals(List.of("0=3", "12=1", "10=215", "11=750", "14=1"), controller.writes);
    }

    @Test
    public void timedFunctions() throws Exception {
        RecordingController controller = new RecordingController();

        FuturaHandler.applyAction(controller, params("id", "futura", "boost", "1800", "party", "0"));

        Assert.assertEquals(List.of("1=1800", "5=0"), controller.writes);
    }

    @Test
    public void outOfRangeIsRejected() throws Exception {
        RecordingController controller = new RecordingController();

        Assert.assertThrows(IllegalArgumentException.class,
                () -> FuturaHandler.applyAction(controller, params("id", "futura", "vent", "7")));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> FuturaHandler.applyAction(controller, params("id", "futura", "boost", "7201")));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> FuturaHandler.applyAction(controller, params("id", "futura", "tempSet", "31")));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> FuturaHandler.applyAction(controller, params("id", "futura", "humiSet", "60")));
        Assert.assertTrue(controller.writes.isEmpty());
    }
}
