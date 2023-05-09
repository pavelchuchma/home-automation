package org.chuma.homecontroller.base.utils;

public class Utils {
    /**
     * Thread.sleep(millis) ignoring InterruptedException
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
