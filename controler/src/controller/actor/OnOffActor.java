package controller.actor;

import node.NodePin;
import org.apache.log4j.Logger;

public class OnOffActor extends AbstractActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    int value;

    Indicator[] indicators;
    private int retryCount = 5;


    public OnOffActor(String id, NodePin output, int initValue, int onValue, Indicator... indicators) {
        super(id, output, initValue);
        this.onValue = onValue;

        this.value = initValue;
        this.indicators = indicators;
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
    public synchronized void setValue(int val, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        value = val;
        if (setPinValue(output, value, retryCount)) {
            setIndicators(false, actionData);
        }
    }

    public void switchOn(Object actionData) {
        setValue(onValue, actionData);
    }

    public void switchOff(Object actionData) {
        setValue((onValue ^ 1) & 1, actionData);
    }

    public boolean isOn() {
        return value == onValue;
    }

}