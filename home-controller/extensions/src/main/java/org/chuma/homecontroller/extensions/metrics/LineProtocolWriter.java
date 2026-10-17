package org.chuma.homecontroller.extensions.metrics;

import java.util.Locale;

/**
 * Builds InfluxDB line protocol with millisecond timestamps. Every measurement name is prefixed with
 * {@code <prefix>-}. A line without any field is dropped, so a source can call the field methods
 * unconditionally and skip values it does not have (null strings, NaN doubles).
 */
public class LineProtocolWriter {
    private final String prefix;
    private final StringBuilder out = new StringBuilder();

    public LineProtocolWriter(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Starts a line. Tags must be added before fields, the line is appended by {@link Line#end()}.
     */
    public Line line(String measurement, long timestampMs) {
        return new Line(measurement, timestampMs);
    }

    public boolean isEmpty() {
        return out.length() == 0;
    }

    public int length() {
        return out.length();
    }

    public void append(CharSequence rawLines) {
        out.append(rawLines);
    }

    public void clear() {
        out.setLength(0);
    }

    @Override
    public String toString() {
        return out.toString();
    }

    public class Line {
        private final StringBuilder head = new StringBuilder();
        private final StringBuilder fields = new StringBuilder();
        private final long timestampMs;

        private Line(String measurement, long timestampMs) {
            this.timestampMs = timestampMs;
            escapeKey(head, prefix + "-" + measurement);
        }

        public Line tag(String key, String value) {
            if (value != null && !value.isEmpty()) {
                head.append(',');
                escapeKey(head, key);
                head.append('=');
                escapeKey(head, value);
            }
            return this;
        }

        public Line field(String key, long value) {
            startField(key).append(value).append('i');
            return this;
        }

        public Line field(String key, double value) {
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                startField(key).append(formatDouble(value));
            }
            return this;
        }

        public Line field(String key, boolean value) {
            startField(key).append(value);
            return this;
        }

        public Line field(String key, String value) {
            if (value != null) {
                StringBuilder sb = startField(key).append('"');
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (c == '"' || c == '\\') {
                        sb.append('\\');
                    }
                    sb.append(c == '\n' || c == '\r' ? ' ' : c);
                }
                sb.append('"');
            }
            return this;
        }

        public Line field(String key, Enum<?> value) {
            return field(key, value == null ? null : value.name());
        }

        /**
         * Appends the line to the writer; a line without fields is dropped.
         */
        public void end() {
            if (fields.length() == 0) {
                return;
            }
            out.append(head).append(' ').append(fields).append(' ').append(timestampMs).append('\n');
        }

        private StringBuilder startField(String key) {
            if (fields.length() > 0) {
                fields.append(',');
            }
            escapeKey(fields, key);
            return fields.append('=');
        }
    }

    private static String formatDouble(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        String s = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    private static void escapeKey(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',' || c == '=' || c == ' ') {
                sb.append('\\');
            }
            sb.append(c == '\n' || c == '\r' ? '_' : c);
        }
    }
}
