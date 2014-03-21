package node;

public enum Pin {
    pinA0(0),
    pinA1(1),
    pinA2(2),
    pinA3(3),
    pinA4(4),
    pinA5(5),
    pinA6(6),
    pinA7(7),
    pinB0(8),
    pinB1(9),
    pinB2(10),
    pinB3(11),
    pinB4(12),
    pinB5(13),
    pinB6(14),
    pinB7(15),
    pinC0(16),
    pinC1(17),
    pinC2(18),
    pinC3(19),
    pinC4(20),
    pinC5(21),
    pinC6(22),
    pinC7(23),
    pinD0(24),
    pinD1(25),
    pinD2(26),
    pinD3(27),
    pinD4(28),
    pinD5(29),
    pinD6(30),
    pinD7(31);

    private final int index;

    private Pin(int index) {
        this.index = index;
    }

    public char getPort() {
        return (char) ('A' + (index / 8));
    }
    public char getBitMask() {
        return (char) (1 << (index % 8));
    }

    /**
     * Returns index of pin in port (0-7)
     */
    public int getPinIndex() {
        return index % 8;
    }

    public static Pin get(int portIndex, int pinIndex) {
        if (portIndex < 0 || portIndex > 3 || pinIndex < 0 || pinIndex > 7) {
            throw new IllegalArgumentException(String.format("Invalid values for Pin %d:%d", portIndex, pinIndex));
        }
        return values()[portIndex * 8 + pinIndex];
    }

    /**
     * Tries to parse values like: A0, b3, ...
     * @param s
     * @return
     */
    public static Pin fromString(String s) {
        if (s == null || s.length() != 2) {
            throw new IllegalArgumentException(String.format("Cannot parse string with value '%s' as Pin", s));
        }
        int port = s.toUpperCase().charAt(0) - 'A';
        int pin = s.charAt(1) - '0';
        try {
            return get(port, pin);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Cannot parse string with value '%s' as Pin", s), e);
        }
    }
}