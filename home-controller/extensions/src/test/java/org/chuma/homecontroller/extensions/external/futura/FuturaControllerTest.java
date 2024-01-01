package org.chuma.homecontroller.extensions.external.futura;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FuturaControllerTest extends AbstractFuturaTestBase {
    static Logger log = LoggerFactory.getLogger(FuturaControllerTest.class.getName());

    public void testGetState() throws Exception {
        log.debug("create");
        FuturaController fc = new FuturaController(futuraIpAddress);
        log.debug("getState");
        long startTime = System.currentTimeMillis();
        State state = fc.getState();
        long duration = System.currentTimeMillis() - startTime;
        log.debug("getState done in {} ms", duration);

        printState(state);
    }

    public void testSetVentilationSpeed() throws Exception {
        FuturaController fc = new FuturaController(futuraIpAddress);
        // get initial states
        State state = fc.getState();
        int initialVentilationSpeed = state.getVentilationSpeed();
        boolean initialTimeProgramActive = state.getTimeProgramActive();
        log.debug("before: ventilationSpeed={}, timeProgramActive={}", initialVentilationSpeed, initialTimeProgramActive);

        // set new values
        int targetVentilationSpeed = initialVentilationSpeed + 1;
        boolean targetTimeProgramActive = !initialTimeProgramActive;
        log.debug("setting: ventilationSpeed={}, timeProgramActive={}", targetVentilationSpeed, targetTimeProgramActive);
        fc.setVentilationSpeed(targetVentilationSpeed);
        fc.setTimeProgramActive(targetTimeProgramActive);
        Thread.sleep(1_000);

        // get fresh state after change
        state = fc.getState();
        Assert.assertEquals(targetVentilationSpeed, state.getVentilationSpeed());
        Assert.assertEquals(targetTimeProgramActive, state.getTimeProgramActive());

        // restore original value
        log.debug("setting back: ventilationSpeed={}, timeProgramActive={}", initialVentilationSpeed, initialTimeProgramActive);
        fc.setVentilationSpeed(initialVentilationSpeed);
        fc.setTimeProgramActive(initialTimeProgramActive);

        // validate backup restore
        state = fc.getState();
        Assert.assertEquals(initialVentilationSpeed, state.getVentilationSpeed());
        Assert.assertEquals(initialTimeProgramActive, state.getTimeProgramActive());
    }

    private static void printState(State state) {
        log.debug("*** STATE ***");
        log.debug("getVentilationSpeed: {}", state.getVentilationSpeed());
        log.debug("getAirTempAmbient: {}", state.getAirTempAmbient());
        log.debug("getAirTempFresh: {}", state.getAirTempFresh());
        log.debug("getAirTempIndoor: {}", state.getAirTempIndoor());
        log.debug("getAirTempWaste: {}", state.getAirTempWaste());
        log.debug("getFilterWearLevelPercent: {}%", state.getFilterWearLevelPercent());
        log.debug("getPowerConsumption: {}W", state.getPowerConsumption());
        log.debug("getHeatRecovering: {}W", state.getHeatRecovering());
        log.debug("getWallControllerCO2: {}", state.getWallControllerCO2());
        log.debug("getWallControllerTemperature: {}C", state.getWallControllerTemperature());
        log.debug("getTimeProgramActive: {}", state.getTimeProgramActive());
    }
}