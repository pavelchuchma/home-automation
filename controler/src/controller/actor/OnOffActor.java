package controller.actor;

import node.NodePin;
import org.apache.log4j.Logger;

public class OnOffActor extends AbstractActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    int value;

    Indicator[] indicators;
    private int retryCount = 5;

    OnOffActor conflictingActor;

    public OnOffActor(String id, NodePin output, int initValue, int onValue, Indicator... indicators) {
        super(id, output, initValue);
        this.onValue = onValue;

        this.value = initValue;
        this.indicators = indicators;
    }

    public void setConflictingActor(OnOffActor conflictingActor) {
        this.conflictingActor = conflictingActor;
    }

    public String toString() {
        StringBuilder val = new StringBuilder(String.format("OnOffActor(%s) %s", id, output));
        if (indicators != null) {
            val.append(", indicators: ");
            for (Indicator i : indicators) {
                val.append(i.getPin());
                val.append(", ");
            }
        }
        return val.toString();
    }

/*    @Override
    public NodePin[] getOutputPins() {
        if (indicators == null) {
            return new NodePin[]{output};
        } else {
            NodePin[] res = new NodePin[1 + indicators.length];
            res[0] = output;
            for (int i = 0; i < indicators.length; i++) {
                res[i + 1] = indicators[i].getPin();
            }
            return res;
        }
    }
*/
    @Override
    public int getValue() {
        return value;
    }

    /**
     *
     * @param invert - used for blinking
     * @param actionData
     */
    public synchronized void setIndicators(boolean invert, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        if (indicators != null) {
            for (Indicator i : indicators) {
                int indVal = (i.IsInverted() ^ invert) ? (value ^ 1) & 1 : value;
                setPinValue(i.getPin(), indVal, retryCount);
            }
        }
    }

    @Override
    public synchronized boolean setValue(int val, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        // turn off conflicting actor if turning on
        if (conflictingActor != null && val == onValue) {
            if (!conflictingActor.switchOff(actionData)) {
                log.error(String.format("Actor %s: Cannot switch off conflicting actor %s", getId(), conflictingActor.getId()));
                return false;
            }
        }

        if (setPinValue(output, val, retryCount)) {
            value = val;
            setIndicators(false, actionData);
            return true;
        }
        return false;
    }

    public boolean switchOn(Object actionData) {
        log.debug("switchOn: " + toString());
        return setValue(onValue, actionData);
    }

    public boolean switchOff(Object actionData) {
        log.debug("switchOff: " + toString());
        return setValue((onValue ^ 1) & 1, actionData);
    }

    public boolean isOn() {
        return value == onValue;
    }

}