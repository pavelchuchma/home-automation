package org.chuma.homecontroller.extensions.external.boiler;

/**
 * Key sequences driven by the gateway on coil / holding register writes. Mirrors the KEY_SEQUENCE
 * enum of the gateway firmware (coolwex-remote-control, include/types.h); the order defines the
 * values reported in the iregLastFailSequence register.
 */
public enum KeySequence {
    none,
    refreshStatus,
    powerOn,
    setTargetTemp,
    eHeatOn
}
