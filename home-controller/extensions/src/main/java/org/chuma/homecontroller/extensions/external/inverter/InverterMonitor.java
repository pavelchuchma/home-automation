package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterMonitor {
    void start();

    void stop();

    /**
     * Get last known state. Can be null in case of the first request or if it was not used longer than maxUnusedRunTimeMs time.
     */
    InverterState getState();

    /**
     * Returns recent (fresh) status or synchronously gets a new one.
     */
    InverterState getStateSync();
}
