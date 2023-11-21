package org.chuma.homecontroller.extensions.external.futura;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;

public class FuturaMonitor extends AbstractStateMonitor<State> {
    private static final Logger log = LoggerFactory.getLogger(FuturaMonitor.class.getName());
    final FuturaController controller;

    public FuturaMonitor(String ipAddress, int refreshInternalMs, int maxUnusedRunTimeMs) {
        super("FuturaMonitor", refreshInternalMs, maxUnusedRunTimeMs);
        log.info("Creating FuturaMonitor for {}", ipAddress);

        FuturaController ctrl;
        try {
            ctrl = new FuturaController(ipAddress);
        } catch (UnknownHostException e) {
            ctrl = null;
            log.error("Failed to create FuturaController from '" + ipAddress + "'", e);
        }
        this.controller = ctrl;
    }

    @Override
    protected State getStateImpl() {
        try {
            long startTime = 0;
            if (log.isTraceEnabled()) {
                startTime = System.currentTimeMillis();
            }
            log.debug("refreshing Futura state");
            State state = controller.getState();
            log.trace("done in {} ms, ventilation speed: {}", System.currentTimeMillis() - startTime, state.getVentilationSpeed());
            return state;
        } catch (Exception e) {
            log.error("Failed to refresh Futura state", e);
            return null;
        }
    }
}
