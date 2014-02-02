package controller;

import controller.actor.AbstractActor;

public class Action {
    enum Type {
        TurnOn,
        TurnOff,
        ChangeOnOff,
    }

    AbstractActor actor;
    Type actionType;

}