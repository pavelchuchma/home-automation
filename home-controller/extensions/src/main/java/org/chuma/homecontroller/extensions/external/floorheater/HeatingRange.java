package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Absolute time range in which a floor heater is on, half-open: [from, to).
 * Serialized as "P2026-11-04T02:15/2026-11-04T02:30;M2026-11-03T21:45/2026-11-04T03:45",
 * the prefix is the {@link Kind}.
 */
public record HeatingRange(LocalDateTime from, LocalDateTime to, Kind kind) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public enum Kind {
        /** chosen by the planner */
        PLANNED('P'),
        /** made by the on/off switch or edited by hand */
        MANUAL('M');

        final char prefix;

        Kind(char prefix) {
            this.prefix = prefix;
        }

        static Kind ofPrefix(char c) {
            for (Kind k : values()) {
                if (k.prefix == c) {
                    return k;
                }
            }
            throw new IllegalArgumentException("Unknown range kind '" + c + "'");
        }
    }

    public boolean contains(LocalDateTime time) {
        return !time.isBefore(from) && time.isBefore(to);
    }

    public boolean isValid() {
        return to.isAfter(from);
    }

    @Override
    public String toString() {
        return kind.prefix + FORMAT.format(from) + "/" + FORMAT.format(to);
    }

    public static String format(List<HeatingRange> ranges) {
        StringBuilder sb = new StringBuilder();
        for (HeatingRange r : ranges) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(r);
        }
        return sb.toString();
    }

    public static List<HeatingRange> parse(String str) {
        List<HeatingRange> result = new ArrayList<>();
        if (str == null || str.isBlank()) {
            return result;
        }
        for (String item : str.split(";")) {
            String s = item.trim();
            String[] times = s.substring(1).split("/");
            Validate.isTrue(times.length == 2, "Invalid heating range '%s'", s);
            result.add(new HeatingRange(LocalDateTime.parse(times[0]), LocalDateTime.parse(times[1]), Kind.ofPrefix(s.charAt(0))));
        }
        return result;
    }

    /**
     * Sorted copy with overlapping and touching ranges merged; a merge with a manual range is manual.
     */
    public static List<HeatingRange> normalize(List<HeatingRange> ranges) {
        List<HeatingRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(HeatingRange::from));
        List<HeatingRange> result = new ArrayList<>();
        for (HeatingRange r : sorted) {
            HeatingRange last = result.isEmpty() ? null : result.get(result.size() - 1);
            if (last != null && !r.from.isAfter(last.to)) {
                LocalDateTime to = r.to.isAfter(last.to) ? r.to : last.to;
                Kind kind = (last.kind == Kind.MANUAL || r.kind == Kind.MANUAL) ? Kind.MANUAL : Kind.PLANNED;
                result.set(result.size() - 1, new HeatingRange(last.from, to, kind));
            } else {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * @return error message for the user, null when the ranges are valid (each ends after it starts, no overlaps)
     */
    public static String validate(List<HeatingRange> ranges) {
        for (int i = 0; i < ranges.size(); i++) {
            if (!ranges.get(i).isValid()) {
                return "Range " + (i + 1) + " must end after it starts.";
            }
        }
        List<HeatingRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(HeatingRange::from));
        for (int i = 0; i + 1 < sorted.size(); i++) {
            if (sorted.get(i + 1).from.isBefore(sorted.get(i).to)) {
                return "Ranges " + (ranges.indexOf(sorted.get(i)) + 1) + " and " + (ranges.indexOf(sorted.get(i + 1)) + 1)
                        + " overlap. Merge them or move one.";
            }
        }
        return null;
    }
}
