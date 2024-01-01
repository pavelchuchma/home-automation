package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterMonitor {
    void start();

    void stop();

    /**
     * Get last known state. Can be null in case of the first request or if it was not used longer than maxUnusedRunTimeMs time.
     */
    InverterState getState();

    /**
     * Returns stored State or synchronously gets new one if no valid state is stored.
     * @param forceRefresh force getting fresh state, regardless valid state is already stored.
     */    InverterState getStateSync(boolean forceRefresh);
}
