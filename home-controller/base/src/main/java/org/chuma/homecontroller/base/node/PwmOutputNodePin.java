package org.chuma.homecontroller.base.node;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.packet.Packet;

public class PwmOutputNodePin extends NodePin {
    private int maxPwmValue;

    /**
     * @param id   pin ID, should be globally unique although not required
     * @param name pin name inside its "parent" device
     * @param node node pin is connected to
     * @param pin  pin in node
     * @param maxPwmValue Maximum allowed pwmValue on the pin. Expected range:
     *                    <0;{@link org.chuma.homecontroller.base.packet.Packet.MAX_PWM_VALUE}>
     */
    public PwmOutputNodePin(String id, String name, Node node, Pin pin, int maxPwmValue) {
        super(id, name, node, pin);
        Validate.inclusiveBetween(0, Packet.MAX_PWM_VALUE, maxPwmValue);
        this.maxPwmValue = maxPwmValue;
    }

    public int getMaxPwmValue() {
        return maxPwmValue;
    }

    public void setMaxPwmValue(int maxPwmValue) {
        Validate.inclusiveBetween(0, Packet.MAX_PWM_VALUE, maxPwmValue);
        this.maxPwmValue = maxPwmValue;
    }
}
