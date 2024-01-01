package org.chuma.homecontroller.extensions.external.futura;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitorTest;

public class FuturaMonitorTest extends AbstractFuturaTestBase {
    public void testMonitor() {
        FuturaMonitor monitor = new FuturaMonitor(futuraIpAddress, 3_000, 8_000);

        AbstractStateMonitorTest.processMonitorStateTest(monitor, 100);
    }

    public void testGetBeforeStart() throws Exception {
        FuturaMonitor monitor = new FuturaMonitor(futuraIpAddress, 3_000, 8_000);

        AbstractStateMonitorTest.processGetBeforeStartTest(monitor, 100);
    }
}