package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterMonitor {
    void start();

    void stop();

    /**
     * Get last known state. Can be null before the first refresh or when the last refresh failed.
     */
    InverterState getState();

    /**
     * Returns stored State or synchronously gets new one if no valid state is stored.
     *
     * @param forceRefresh force getting fresh state, regardless valid state is already stored.
     */
    InverterState getStateSync(boolean forceRefresh);
}
