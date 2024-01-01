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
 * Refresh thread is stopped if monitor is not used for a specified time and restarted after next request.
 */
public class SolaxInverterMonitor extends AbstractStateMonitor<InverterState> implements InverterMonitor {
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterMonitor.class.getName());

    protected final SolaxInverterModbusClient client;

    public SolaxInverterMonitor(SolaxInverterModbusClient client, int refreshInternalMs, int maxUnusedRunTimeMs) {
        super("SolaxInverterMonitor", refreshInternalMs, maxUnusedRunTimeMs);
        this.client = client;
    }

    @Override
    protected InverterState getStateImpl() {
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
