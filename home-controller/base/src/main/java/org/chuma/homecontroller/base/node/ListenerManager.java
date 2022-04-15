package org.chuma.homecontroller.base.node;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Keeps list of listeners and provides methods to call them.
 */
public class ListenerManager<T> {
    protected final Queue<T> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Register new listener.
     */
    public void add(T listener) {
        listeners.add(listener);
    }

    /**
     * Unregister listener.
     */
    public void remove(T listener) {
        listeners.remove(listener);
    }

    /**
     * Call listeners using passed lambda function.
     */
    public void callListeners(Consumer<T> call) {
        for (T listener : listeners) {
            call.accept(listener);
        }
    }

    /**
     * Call listeners using passed lambda function.
     */
    public <E extends Exception> void callListenersWithException(ConsumerWithException<T, E> call) throws E {
        for (T listener : listeners) {
            call.accept(listener);
        }
    }

    public static interface ConsumerWithException<T, E extends Exception> {
        void accept(T listener) throws E;
    }
}
