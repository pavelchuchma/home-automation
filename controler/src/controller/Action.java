package controller;

public class Action {
    enum Type {
        TurnOn,
        TurnOff,
        ChangeOnOff,
    }
    enum Foo {
        BAR (0),
        BAZ (1),
        FII (10);

        private final int anInt;

        Foo(int index) {
            this.anInt = index;
        }

    }
    Actor actor;
    Type actionType;

    Action() {
        Foo a = Foo.BAR;
    }
}