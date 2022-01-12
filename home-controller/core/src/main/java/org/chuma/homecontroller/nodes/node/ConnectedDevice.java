package org.chuma.homecontroller.nodes.node;

public interface ConnectedDevice {
    int getConnectorNumber();
    CpuFrequency getRequiredCpuFrequency();
    int getEventMask();
    int getOutputMasks();
    int getInitialOutputValues();
}
