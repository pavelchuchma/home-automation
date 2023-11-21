package org.chuma.homecontroller.extensions.external;

import junit.framework.TestCase;

public class AbstractStateMonitorTest extends TestCase {
    /**
     * Private field getter for tests
     */
    public static <S> S getRawState(AbstractStateMonitor<S> m) {
        return m.state;
    }

}