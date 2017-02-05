package controller.action;

import controller.actor.Actor;
import controller.actor.IOnOffActor;

public class SwitchAllOffWithMemory implements Action {

    final Action[] switchOffActions;
    final IOnOffActor[] actors;
    int[] lastValues;

    public SwitchAllOffWithMemory(IOnOffActor... actors) {
        this.actors = actors;
        this.switchOffActions = createSwitchOffActions(actors);
        lastValues = new int[actors.length];
    }

    public static Action[] createSwitchOffActions(IOnOffActor... actors) {
        Action[] actions = new Action[actors.length];
        for (int i = 0; i < actors.length; i++) {
            actions[i] = new SwitchOffAction(actors[i]);
        }
        return actions;
    }

    @Override
    public void perform(int previousDurationMs) {
        if (isAllOff()) {
            for (int i = 0; i < actors.length; i++) {
                if (lastValues[i] != actors[i].getValue()) {
                    actors[i].setValue(lastValues[i], null);
                }
            }
        } else {
            for (int i = 0; i < actors.length; i++) {
                lastValues[i] = actors[i].getValue();
            }
            for (Action switchOffAction : switchOffActions) {
                switchOffAction.perform(-1);
            }
        }
    }

    @Override
    public Actor getActor() {
        return null;
    }

    private boolean isAllOff() {
        for (IOnOffActor actor : actors) {
            if (actor.isOn()) {
                return false;
            }
        }
        return true;
    }
}