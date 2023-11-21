package org.chuma.homecontroller.extensions.external.futura;

public class State {
    private final int[] inputRegisters;
    private final int[] holdingRegisters;

    public State(int[] inputRegisters, int[] holdingRegisters) {
        this.inputRegisters = inputRegisters;
        this.holdingRegisters = holdingRegisters;
    }

    /**
     * Set ventilation power (0 – off, 1..5 – preset
     * level 1 to 5, 6 – automatic ventilation)
     */
    public int getVentilationSpeed() {
        return holdingRegisters[0];
    }

    public double getAirTempAmbient() {
        return inputRegisters[30] / 10d;
    }

    public double getAirTempFresh() {
        return inputRegisters[31] / 10d;
    }

    public double getAirTempIndoor() {
        return inputRegisters[32] / 10d;
    }

    public double getAirTempWaste() {
        return inputRegisters[33] / 10d;
    }

    public int getFilterWearLevelPercent() {
        return inputRegisters[40];
    }

    /**
     * Current power consumption of the unit in watts
     */
    public int getPowerConsumption() {
        return inputRegisters[41];
    }

    /**
     * Current value of heat recovery in watts
     */
    public int getHeatRecovering() {
        return inputRegisters[42];
    }

    /**
     * The CO2 value of the wall controller in ppm
     */
    public int getWallControllerCO2() {
        return inputRegisters[102];
    }

    /**
     * Wall controller temperature
     */
    public  double getWallControllerTemperature() {
        return inputRegisters[103] / 10d;
    }

    public boolean getTimeProgramActive() {
        return holdingRegisters[12] == 1;
    }

    @SuppressWarnings("unused")
    void printAll() {
        for (int i = 0; i < inputRegisters.length; i++) {
            System.out.println("input " + i + "=" + inputRegisters[i]);
        }
        for (int i = 0; i < holdingRegisters.length; i++) {
            System.out.println("holding " + i + "=" + holdingRegisters[i]);
        }
    }
}
