package org.chuma.homecontroller.base.packet.simulation;

import org.slf4j.event.Level;

/**
 * Listener for events from simulator.
 */
public interface SimulatedNodeListener {
    /**
     * Log message reported by simulator.
     *
     * @param node          node for which message is reported
     * @param level         log level, but should report only {@link Level#INFO} and higher, not debug
     * @param messageFormat message format according to {@link String#format(String, Object...)}
     * @param args
     */
    void logMessage(SimulatedNode node, Level level, String messageFormat, Object ... args);
    /**
     * Port value was set.
     */
    void onSetPort(SimulatedNode node, int port, int value);
    /**
     * TRIS value was set.
     */
    void onSetTris(SimulatedNode node, int port, int value);
    /**
     * Event mask was set.
     */
    void onSetEventMask(SimulatedNode node, int port, int mask);
    /**
     * Manual PWM was set for pin.
     */
    void onSetManualPwm(SimulatedNode node, int port, int pin, int value);
}
