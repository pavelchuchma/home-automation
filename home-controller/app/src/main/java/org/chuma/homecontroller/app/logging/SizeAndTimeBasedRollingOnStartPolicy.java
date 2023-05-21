package org.chuma.homecontroller.app.logging;

import java.io.File;

import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

/**
 * Enforces rolling on application start. Tested with logback 1.3.7.
 */
public class SizeAndTimeBasedRollingOnStartPolicy<E> extends SizeAndTimeBasedRollingPolicy<E> {
    private int initializationPhase = 0;
    private FileSize originalMaxSize = null;

    @Override
    public void setMaxFileSize(FileSize aMaxFileSize) {
        originalMaxSize = aMaxFileSize;
        super.setMaxFileSize(new FileSize(1));
    }

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        if (initializationPhase == 0) {
            initializationPhase++;
        } else if (initializationPhase == 1) {
            initializationPhase++;
            stop();
            super.setMaxFileSize(originalMaxSize);
            start();
        }
        return super.isTriggeringEvent(activeFile, event);
    }
}
