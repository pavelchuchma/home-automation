package org.chuma.homecontroller.base.node;

public class NodeConfigurationException extends IllegalArgumentException {
    private final String code;

    public NodeConfigurationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
