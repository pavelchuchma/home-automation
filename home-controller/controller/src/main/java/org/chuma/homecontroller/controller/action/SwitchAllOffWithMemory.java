package org.chuma.homecontroller.controller.action;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.actor.IContinuousValueActor;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchAllOffWithMemory implements Action {
    final Item<?, ?>[] items;

    public SwitchAllOffWithMemory(IOnOffActor... actors) {
        items = new Item[actors.length];
        for (int i = 0; i < actors.length; i++) {
            if (actors[i] instanceof IContinuousValueActor) {
                items[i] = new Item<>((IContinuousValueActor)actors[i], IContinuousValueActor::getValue, (a, v) -> a.setValue(v, null));
            } else {
                items[i] = new Item<>(actors[i], IOnOffActor::isOn, (a, v) -> {
                    if (v) {
                        a.switchOn(null);
                    } else {
                        a.switchOff(null);
                    }
                });
            }
        }
    }

    public static Action[] createSwitchOffActions(IOnOffActor... actors) {
        return Arrays.stream(actors).map(SwitchOffAction::new).toArray(Action[]::new);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        if (isAllOff()) {
            Arrays.stream(items).forEach(Item::restoreState);
        } else {
            Arrays.stream(items).forEach(item -> {
                item.saveState();
                item.actor.switchOff(null);
            });
        }
    }

    @Override
    public Actor getActor() {
        return null;
    }

    private boolean isAllOff() {
        return Arrays.stream(items).noneMatch(i -> i.actor.isOn());
    }

    /**
     * Holds a generic actor and its state
     */
    private static class Item<A extends IOnOffActor, V> {
        final A actor;
        private final Function<A, V> saveState;
        private final BiConsumer<A, V> restoreState;
        V savedValue;

        public Item(A actor, Function<A, V> saveState, BiConsumer<A, V> restoreState) {
            this.actor = actor;
            this.saveState = saveState;
            this.restoreState = restoreState;
        }

        public void saveState() {
            savedValue = saveState.apply(actor);
        }

        public void restoreState() {
            Validate.isTrue(savedValue != null, "Cannot restore state because it was not saved yet!");
            restoreState.accept(actor, savedValue);
        }
    }
}
