package org.chuma.homecontroller.extensions.external.boiler;

public class State {

    public enum MODE {
        unknown,
        displayOff,
        locked,
        invalid,
        infoT5U,
        infoT5L,
        infoT3,
        infoT4,
        infoTP,
        infoTh,
        infoCE,
        infoER1,
        infoER2,
        infoER3,
        infoD7F,
        setClock,
        setTemp,
        unlocked,
        setVacation,
        vacation
    }

    private final State.MODE displayMode;
    private final int statusAge;
    private final int tempT5U;
    private final int tempT5L;
    private final int tempT3;
    private final int tempT4;
    private final int tempTP;
    private final int tempTh;
    private final int targetTemp;

    private final boolean on;
    private final boolean hot;
    private final boolean eHeat;
    private final boolean pump;
    private final boolean vacation;

    public State(MODE displayMode, int statusAge, int tempT5U, int tempT5L, int tempT3, int tempT4, int tempTP,
                 int tempTh, int targetTemp, boolean on, boolean hot, boolean eHeat, boolean pump, boolean vacation) {
        this.displayMode = displayMode;
        this.statusAge = statusAge;
        this.tempT5U = tempT5U;
        this.tempT5L = tempT5L;
        this.tempT3 = tempT3;
        this.tempT4 = tempT4;
        this.tempTP = tempTP;
        this.tempTh = tempTh;
        this.targetTemp = targetTemp;
        this.on = on;
        this.hot = hot;
        this.eHeat = eHeat;
        this.pump = pump;
        this.vacation = vacation;
    }

    public MODE getDisplayMode() {
        return displayMode;
    }

    public int getStatusAge() {
        return statusAge;
    }

    public int getTempT5U() {
        return tempT5U;
    }

    public int getTempT5L() {
        return tempT5L;
    }

    public int getTempT3() {
        return tempT3;
    }

    public int getTempT4() {
        return tempT4;
    }

    public int getTempTP() {
        return tempTP;
    }

    public int getTempTh() {
        return tempTh;
    }

    public int getTargetTemp() {
        return targetTemp;
    }

    public boolean isOn() {
        return on;
    }

    public boolean isHot() {
        return hot;
    }

    public boolean isEHeat() {
        return eHeat;
    }

    public boolean isPump() {
        return pump;
    }

    public boolean isVacation() {
        return vacation;
    }
}
