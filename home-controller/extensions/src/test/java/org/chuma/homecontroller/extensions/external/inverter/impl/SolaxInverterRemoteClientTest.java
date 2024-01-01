package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.junit.Assert;

public class SolaxInverterRemoteClientTest extends AbstractSolaxInverterTestBase {
    public void testClient() {
        final SolaxInverterRemoteClient client = new SolaxInverterRemoteClient(localIp, localPassword, remoteUsername, remotePasswordToken, basicConfigPin);

        int targetValue = 37;
        client.setSelfUseMinimalSoc(targetValue);

        RemoteConfiguration configuration = client.getConfiguration();
        Assert.assertEquals(targetValue, configuration.getSelfUseMinimalSoc());
    }
}
