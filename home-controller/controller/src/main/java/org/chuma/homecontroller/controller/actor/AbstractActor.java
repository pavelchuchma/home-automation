package org.chuma.homecontroller.controller.actor;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractActor implements Actor, IReadableOnOff {
    protected ActorListener[] actorListeners;
    String id;
    String label;
    Object actionData;

    public AbstractActor(String id, String label, ActorListener... actorListeners) {
        this.label = label;
        this.actorListeners = actorListeners;
        this.id = id;

        for (ActorListener lst : actorListeners) {
            lst.addSource(this);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Object getActionData() {
        return actionData;
    }

    @Override
    public synchronized void setActionData(Object actionData) {
        this.actionData = actionData;
        notifyAll();
    }

    @Override
    public synchronized void callListenersAndSetActionData(Object actionData) {
        setActionData(actionData);

        if (actorListeners != null) {
            for (ActorListener listener : actorListeners) {
                listener.onAction(this, actionData);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s(%s)", getClass().getSimpleName(), id));
        appendListeners(sb);
        return sb.toString();
    }

    protected void appendListeners(StringBuilder sb) {
        if (actorListeners != null) {
            sb.append(", actorListeners: [");
            List<String> listeners = new ArrayList<>();
            for (ActorListener actorListener : actorListeners) {
                listeners.add(actorListener.toString());
            }
            sb.append(String.join(", ", listeners));
            sb.append("]");
        }
    }
}
