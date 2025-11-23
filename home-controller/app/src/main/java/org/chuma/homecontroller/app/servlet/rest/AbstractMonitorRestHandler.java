package org.chuma.homecontroller.app.servlet.rest;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;

public abstract class AbstractMonitorRestHandler<T extends AbstractStateMonitor<?>> extends AbstractRestHandler<T> {
    protected final T monitor;

    public AbstractMonitorRestHandler(String id, T monitor) {
        super(id, monitor);
        this.monitor = monitor;
    }

    @Override
    public boolean isEnabled() {
        return monitor.isRunning();
    }
}
