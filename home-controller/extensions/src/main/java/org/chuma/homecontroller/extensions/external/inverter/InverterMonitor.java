package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterMonitor {
    void start();

    void stop();

    InverterState getState();
}
