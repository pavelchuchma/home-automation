package org.chuma.homecontroller.extensions.external.futura;

/**
 * Functions the unit runs for a given time and then ends by itself. Holding register reads the remaining time,
 * writing it starts the function for that many seconds, 0 ends it.
 */
public enum TimedFunction {
    BOOST(1, 7200),
    CIRCULATION(2, 21600),
    OVERPRESSURE(3, 7200),
    NIGHT(4, 28800),
    PARTY(5, 28800);

    final int register;
    public final int maxSeconds;

    TimedFunction(int register, int maxSeconds) {
        this.register = register;
        this.maxSeconds = maxSeconds;
    }
}
