package org.chuma.homecontroller.app.configurator;

import org.apache.commons.lang3.Validate;

/**
 * Creates and keeps single global instance of {@link Options}.
 */
public class OptionsSingleton {
    private static Options instance;

    public static Options createInstance(String persistenceFile, String defaultResource) {
        Validate.isTrue(instance == null, "Only one instance of " + Options.class.getSimpleName() + " is allowed");
        return instance = new Options( persistenceFile, defaultResource);
    }

    public static Options getInstance() {
        return instance;
    }

    public static String get(String key) {
        return instance.get(key);
    }

    public int getInt(String key) {
        return instance.getInt(key);
    }

    public boolean getBoolean(String key) {
        return instance.getBoolean(key);
    }
}
