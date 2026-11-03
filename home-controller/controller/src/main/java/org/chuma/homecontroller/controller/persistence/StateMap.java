package org.chuma.homecontroller.controller.persistence;

/**
 * Map for storing state
 */
public interface StateMap {
    void setValue(String key, int value);

    void setValue(String key, String value);

    /**
     * Removes entry if present
     */
    void removeValue(String key);

    Integer getValue(String key);

    String getString(String key);
}
