package org.chuma.homecontroller.extensions.external.inverter.impl;

import junit.framework.TestCase;

import org.chuma.homecontroller.app.configurator.Options;

public abstract class AbstractSolaxInverterTestBase extends TestCase {
    protected final String localIp;

    public AbstractSolaxInverterTestBase() {
        Options options = new Options("../cfg/app.properties", "default-app.properties");
        localIp = options.get("inverter.ip");
    }
}