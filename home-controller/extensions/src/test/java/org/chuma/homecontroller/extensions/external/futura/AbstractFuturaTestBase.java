package org.chuma.homecontroller.extensions.external.futura;

import junit.framework.TestCase;

import org.chuma.homecontroller.app.configurator.Options;

public abstract class AbstractFuturaTestBase extends TestCase {
    protected final String futuraIpAddress;
    public AbstractFuturaTestBase() {
        Options options = new Options("../cfg/app.properties", "default-app.properties");
        futuraIpAddress = options.get("futura.ipAddress");
    }
}
