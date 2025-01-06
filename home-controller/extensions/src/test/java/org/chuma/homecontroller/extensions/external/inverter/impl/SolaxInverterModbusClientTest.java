package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public class SolaxInverterModbusClientTest extends AbstractSolaxInverterTestBase {
    static Logger log = LoggerFactory.getLogger(SolaxInverterModbusClientTest.class.getName());

    public void testPerformance() throws Exception {
        SolaxInverterModbusClient client = new SolaxInverterModbusClient(localIp);
        client.getState();
        long startTime = System.currentTimeMillis();
        final int rounds = 15;
        for (int i = 0; i < rounds; i++) {
            InverterState state = client.getState();
        }
        long duration = (System.currentTimeMillis() - startTime);
        log.trace("Total time: {} s, average response time: {} ms, ", (duration) / 1000d, duration / rounds);
    }

    public void testSetSelfUseMinimalSoc() throws Exception {
        SolaxInverterModbusClient client = new SolaxInverterModbusClient(localIp);

        InverterState state = client.getState();
        int initialSelfUseMinimalSoc = state.getSelfUseMinimalSoc();
        log.debug("before: selfUseMinimalSoc={}", initialSelfUseMinimalSoc);

        int targetValue = initialSelfUseMinimalSoc + 5;
        log.debug("setting: selfUseMinimalSoc={}", targetValue);
        client.setSelfUseMinimalSoc(targetValue);

        // get fresh state after change
        state = client.getState();
        Assert.assertEquals(targetValue, state.getSelfUseMinimalSoc());

        // restore original value
        log.debug("setting back: selfUseMinimalSoc={}", initialSelfUseMinimalSoc);
        client.setSelfUseMinimalSoc(initialSelfUseMinimalSoc);
        state = client.getState();
        log.debug("after test: selfUseMinimalSoc={}", state.getSelfUseMinimalSoc());
        Assert.assertEquals(initialSelfUseMinimalSoc, state.getSelfUseMinimalSoc());
    }

    public void testSetPgridBias() throws Exception {
        SolaxInverterModbusClient client = new SolaxInverterModbusClient(localIp);

        InverterState state = client.getState();
        InverterState.PgridBias initialPgridBias = state.getPgridBias();
        log.debug("before: pgridBias={}", initialPgridBias);

        InverterState.PgridBias targetValue = (initialPgridBias == InverterState.PgridBias.Disable) ? InverterState.PgridBias.Grid : InverterState.PgridBias.Disable;
        log.debug("setting: pgridBias={}", targetValue);
        client.setPgridBias(targetValue);

        // get fresh state after change
        state = client.getState();
        Assert.assertEquals(targetValue, state.getPgridBias());

        // restore original value
        log.debug("setting back: pgridBias={}", initialPgridBias);
        client.setPgridBias(initialPgridBias);
        state = client.getState();
        log.debug("after test: pgridBias={}", state.getPgridBias());
        Assert.assertEquals(initialPgridBias, state.getPgridBias());
    }
}
