package org.chuma.homecontroller.extensions.external.boiler;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;

public class BoilerMonitor extends AbstractStateMonitor<State> {
    private static final Logger log = LoggerFactory.getLogger(BoilerMonitor.class.getName());
    final BoilerController controller;

    public BoilerMonitor(String ipAddress, int refreshInternalMs, int maxUnusedRunTimeMs) {
        super("BoilerMonitor", refreshInternalMs, maxUnusedRunTimeMs);
        log.info("Creating BoilerMonitor for {}", ipAddress);

        BoilerController ctrl;
        try {
            ctrl = new BoilerController(ipAddress);
        } catch (UnknownHostException e) {
            ctrl = null;
            log.error("Failed to create BoilerController from '{}'", ipAddress, e);
        }
        this.controller = ctrl;
    }

    public BoilerController getController() {
        return controller;
    }

    @Override
    protected State getStateImpl() {
        try {
            long startTime = 0;
            if (log.isTraceEnabled()) {
                startTime = System.currentTimeMillis();
            }
            log.debug("refreshing Boiler state");
            controller.refreshStatus();
            State state = controller.getState();
            log.trace("done in {} ms, display mode: {}", System.currentTimeMillis() - startTime, state.getDisplayMode());
            return state;
        } catch (Exception e) {
            log.error("Failed to refresh Boiler state", e);
            return null;
        }
    }
}
