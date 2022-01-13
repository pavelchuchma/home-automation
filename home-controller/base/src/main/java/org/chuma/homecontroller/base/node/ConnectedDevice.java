package org.chuma.homecontroller.base.node;

public interface ConnectedDevice {
    int getConnectorNumber();

    CpuFrequency getRequiredCpuFrequency();

    int getEventMask();

    int getOutputMasks();

    int getInitialOutputValues();
}
