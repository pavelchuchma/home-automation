package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;

/**
 * Monitoring service of Solax X3-Hybrid G4 Inverter
 * <p>
 * It runs periodic refresh in a background thread because inverter response time is ~1.2s.
 * <p>
 * The refresh rate drops to the idle interval when the monitor is not used for a specified time.
 */
public class SolaxInverterMonitor extends AbstractStateMonitor<InverterState> implements InverterMonitor {
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterMonitor.class.getName());

    protected final SolaxInverterModbusClient client;

    public SolaxInverterMonitor(SolaxInverterModbusClient client, int refreshIntervalMs, int fastRateDurationMs, int idleRefreshIntervalMs) {
        super("SolaxInverterMonitor", refreshIntervalMs, fastRateDurationMs, idleRefreshIntervalMs);
        this.client = client;
    }

    @Override
    protected InverterState getStateImpl(boolean firstRefresh) {
        try {
            long startTime = 0;
            if (log.isTraceEnabled()) {
                startTime = System.currentTimeMillis();
            }
            log.debug("refreshing solax state");
            InverterState state = client.getState();
            log.trace("done in {} ms, inverter state {}", System.currentTimeMillis() - startTime, state.getMode());
            return state;
        } catch (Exception e) {
            log.error("Failed to refresh SolaxInverter state", e);
            return null;
        }
    }
}
