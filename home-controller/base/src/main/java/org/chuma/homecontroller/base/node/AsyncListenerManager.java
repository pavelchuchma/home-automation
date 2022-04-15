package org.chuma.homecontroller.base.node;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps list of listeners and provides methods to call them asynchronously (in separate thread/executor).
 * Does not wait for their completion.
 */
public class AsyncListenerManager<T> extends ListenerManager<T> {
    private static Logger log = LoggerFactory.getLogger(AsyncListenerManager.class.getName());

    @Override
    public void callListeners(Consumer<T> call) {
        for (T listener : listeners) {
            new Thread(() -> {
                try {
                    call.accept(listener);
                } catch  (Exception e) {
                    log.warn("Exception while executing listener", e);
                }
            }).start();
        }
    }

    @Override
    public <E extends Exception> void callListenersWithException(ConsumerWithException<T, E> call) throws E {
        for (T listener : listeners) {
            new Thread(() -> {
                try {
                    call.accept(listener);
                } catch  (Exception e) {
                    log.warn("Exception while executing listener", e);
                }
            }).start();
        }
    }
}
