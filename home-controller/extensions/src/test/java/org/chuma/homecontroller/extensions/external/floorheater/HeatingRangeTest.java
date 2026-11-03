package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.floorheater.HeatingRange.Kind;

public class HeatingRangeTest {
    private static LocalDateTime t(String s) {
        return LocalDateTime.parse(s);
    }

    @Test
    public void testParseFormatRoundTrip() {
        String str = "P2026-11-04T02:15/2026-11-04T02:30;M2026-11-03T21:45/2026-11-04T03:45";
        List<HeatingRange> ranges = HeatingRange.parse(str);
        Assert.assertEquals(2, ranges.size());
        Assert.assertEquals(new HeatingRange(t("2026-11-04T02:15"), t("2026-11-04T02:30"), Kind.PLANNED), ranges.get(0));
        Assert.assertEquals(new HeatingRange(t("2026-11-03T21:45"), t("2026-11-04T03:45"), Kind.MANUAL), ranges.get(1));
        Assert.assertEquals(str, HeatingRange.format(ranges));
        Assert.assertTrue(HeatingRange.parse("").isEmpty());
        Assert.assertTrue(HeatingRange.parse(null).isEmpty());
        Assert.assertEquals("", HeatingRange.format(List.of()));
    }

    @Test
    public void testFormatDropsSeconds() {
        HeatingRange r = new HeatingRange(t("2026-11-03T21:45:17"), t("2026-11-04T03:45:17"), Kind.MANUAL);
        Assert.assertEquals("M2026-11-03T21:45/2026-11-04T03:45", r.toString());
    }

    @Test
    public void testContains() {
        HeatingRange r = new HeatingRange(t("2026-11-04T02:15"), t("2026-11-04T02:30"), Kind.PLANNED);
        Assert.assertTrue(r.contains(t("2026-11-04T02:15")));
        Assert.assertTrue(r.contains(t("2026-11-04T02:29:59")));
        Assert.assertFalse(r.contains(t("2026-11-04T02:30")));
        Assert.assertFalse(r.contains(t("2026-11-04T02:14:59")));
    }

    @Test
    public void testNormalizeMergesOverlappingAndTouching() {
        List<HeatingRange> ranges = HeatingRange.parse(
                "P2026-11-04T03:00/2026-11-04T04:00;M2026-11-03T21:45/2026-11-04T03:30;P2026-11-04T04:00/2026-11-04T04:15;P2026-11-04T06:00/2026-11-04T06:15");
        List<HeatingRange> normalized = HeatingRange.normalize(ranges);
        Assert.assertEquals("M2026-11-03T21:45/2026-11-04T04:15;P2026-11-04T06:00/2026-11-04T06:15", HeatingRange.format(normalized));
        // the input is not modified
        Assert.assertEquals(4, ranges.size());
    }

    @Test
    public void testValidate() {
        Assert.assertNull(HeatingRange.validate(HeatingRange.parse("P2026-11-04T02:15/2026-11-04T02:30;P2026-11-04T02:30/2026-11-04T03:00")));
        Assert.assertEquals("Range 1 must end after it starts.",
                HeatingRange.validate(HeatingRange.parse("P2026-11-04T02:30/2026-11-04T02:30")));
        Assert.assertEquals("Range 2 must end after it starts.",
                HeatingRange.validate(HeatingRange.parse("P2026-11-04T02:15/2026-11-04T02:30;P2026-11-04T03:00/2026-11-04T02:45")));
        Assert.assertEquals("Ranges 2 and 1 overlap. Merge them or move one.",
                HeatingRange.validate(HeatingRange.parse("P2026-11-04T02:15/2026-11-04T02:45;M2026-11-04T01:00/2026-11-04T02:30")));
    }
}
