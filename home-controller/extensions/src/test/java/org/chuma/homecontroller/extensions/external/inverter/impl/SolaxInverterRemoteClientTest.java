package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.junit.Assert;
import org.junit.Test;

public class SolaxInverterRemoteClientTest extends SolaxInverterTestBase {
    @Test
    public void testClient() {
        final SolaxInverterRemoteClient client = new SolaxInverterRemoteClient(localUrl, localPassword, remoteUsername, remotePasswordToken, basicConfigPin);

        int targetValue = 30;
        client.setSelfUseMinimalSoc(targetValue);

        RemoteConfiguration configuration = client.getConfiguration();
        Assert.assertEquals(targetValue, configuration.getSelfUseMinimalSoc());
    }
}
