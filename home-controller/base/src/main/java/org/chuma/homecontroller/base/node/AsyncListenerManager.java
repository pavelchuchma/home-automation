package org.chuma.homecontroller.base.node;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps list of listeners and provides methods to call them asynchronously (in separate thread/executor).
 * Does not wait for their completion.
 */
public class AsyncListenerManager<T> extends ListenerManager<T> {
    private static final Logger log = LoggerFactory.getLogger(AsyncListenerManager.class.getName());
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void callListeners(Consumer<T> call) {
        for (T listener : listeners) {
            executor.execute(() -> {
                try {
                    call.accept(listener);
                } catch  (Exception e) {
                    log.warn("Exception while executing listener", e);
                }
            });
        }
    }

    @Override
    public <E extends Exception> void callListenersWithException(ConsumerWithException<T, E> call) {
        for (T listener : listeners) {
            executor.execute(() -> {
                try {
                    call.accept(listener);
                } catch  (Exception e) {
                    log.warn("Exception while executing listener", e);
                }
            });
        }
    }
}
