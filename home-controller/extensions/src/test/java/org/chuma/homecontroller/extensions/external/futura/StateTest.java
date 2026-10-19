package org.chuma.homecontroller.extensions.external.futura;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

public class StateTest {
    private static State state(int[] input, int[] holding) {
        return new State(new ModbusClient.Registers(input), new ModbusClient.Registers(holding));
    }

    @Test
    public void flagsAreReadLowWordFirst() {
        int[] input = new int[200];
        input[16] = 0x0104;
        input[17] = 0x0002;
        input[21] = 0x0002;
        State s = state(input, new int[18]);

        // bit 8 "device is on" is always set
        Assert.assertEquals(0x20104L, s.getModeFlags());
        Assert.assertEquals(0L, s.getErrorFlags());
        Assert.assertEquals(1L << 17, s.getWarningFlags());
    }

    @Test
    public void scaledValues() {
        int[] input = new int[200];
        input[34] = 862;
        input[33] = 0xFFEC;
        input[104] = 431;
        int[] holding = new int[18];
        holding[10] = 215;
        holding[11] = 500;
        holding[1] = 1420;
        State s = state(input, holding);

        Assert.assertEquals(86.2, s.getHumidityAmbient(), 1e-9);
        Assert.assertEquals(-2.0, s.getAirTempWaste(), 1e-9);
        Assert.assertEquals(43.1, s.getWallControllerHumidity(), 1e-9);
        Assert.assertEquals(21.5, s.getPreferredTemperature(), 1e-9);
        Assert.assertEquals(50, s.getPreferredHumidity());
        Assert.assertEquals(1420, s.getRemainingSeconds(TimedFunction.BOOST));
        Assert.assertEquals(0, s.getRemainingSeconds(TimedFunction.PARTY));
    }
}
