package org.chuma.homecontroller.controller.device;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.OutputNodePin;

/**
 * Relay board built from {@link GenericOutputDevice}s; subclasses map connector pins to relay numbers.
 */
public abstract class AbstractRelayBoardDevice {
    private final OutputNodePin[] relays;

    protected AbstractRelayBoardDevice(int relayCount) {
        relays = new OutputNodePin[relayCount];
    }

    protected void setRelays(int firstRelay, OutputNodePin... pins) {
        System.arraycopy(pins, 0, relays, firstRelay - 1, pins.length);
    }

    protected static String[] generateNames(String prefix, String[] suffixes) {
        return Arrays.stream(suffixes).map(s -> prefix + "-out" + s).toArray(String[]::new);
    }

    /**
     * @param i Relay index in range <1;relayCount>
     * Attempt to get a relay bound to unused port throws an exception
     */
    public OutputNodePin getRelay(int i) {
        Validate.inclusiveBetween(1, relays.length, i);
        Validate.isTrue(relays[i - 1] != null, "Relay %d is on an unused port.", i);
        return relays[i - 1];
    }
}
