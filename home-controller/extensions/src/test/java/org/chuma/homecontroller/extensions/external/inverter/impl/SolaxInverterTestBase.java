package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.chuma.homecontroller.app.configurator.Options;

public abstract class SolaxInverterTestBase {
    protected final String localPassword;
    protected final String localUrl;
    protected final String remoteUsername;
    protected final String remotePasswordToken;
    protected final String basicConfigPin;

    public SolaxInverterTestBase() {
        Options options = new Options("../cfg/app.properties", "default-pi.properties");
        localPassword = options.get("inverter.local.password");
        localUrl = options.get("inverter.local.url");
        remoteUsername = options.get("inverter.remote.username");
        remotePasswordToken = options.get("inverter.remote.password.token");
        basicConfigPin = options.get("inverter.config.pin.basic");
    }
}