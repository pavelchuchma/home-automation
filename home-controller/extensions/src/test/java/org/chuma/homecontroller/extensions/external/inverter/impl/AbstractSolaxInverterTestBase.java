package org.chuma.homecontroller.extensions.external.inverter.impl;

import junit.framework.TestCase;

import org.chuma.homecontroller.app.configurator.Options;

public abstract class AbstractSolaxInverterTestBase extends TestCase {
    protected final String localPassword;
    protected final String localIp;
    protected final String basicConfigPin;

    public AbstractSolaxInverterTestBase() {
        Options options = new Options("../cfg/app.properties", "default-app.properties");
        localPassword = options.get("inverter.local.password");
        localIp = options.get("inverter.ip");
        basicConfigPin = options.get("inverter.config.pin.basic");
    }
}