package org.chuma.homecontroller.extensions.external.inverter;

import java.time.LocalDate;
import java.time.ZoneId;

import junit.framework.TestCase;

import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

/**
 * Unit tests for the pure {@link InverterManager#shouldDischargeNow} decision function.
 * No inverter hardware or network access is involved.
 */
public class ForceDischargeTest extends TestCase {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int CAPACITY_WH = 10000;
    private static final int DISCHARGE_W = 5900; // -> 1475 Wh per 15-min segment

    private static long at(int hour, int minute) {
        return LocalDate.now().atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static long tomorrowAt(int hour, int minute) {
        return LocalDate.now().plusDays(1).atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static IntervalPrice seg(long time, double price) {
        return new IntervalPrice(time, price, 0, 0);
    }

    /** soc 50 -> minimalSoc 20 = 3000 Wh usable = ceil(3000/1475) = 3 segments needed. */
    public void testCurrentSegmentAmongTopSelected() {
        IntervalPrice[] prices = {
                seg(at(18, 0), 10),  // current
                seg(at(18, 15), 4),
                seg(at(18, 30), 9),
                seg(at(18, 45), 3),
                seg(at(19, 0), 8),
                seg(at(19, 15), 2),
        };
        // top 3 by price: 10 (18:00), 9 (18:30), 8 (19:00) -> current 18:00 is selected
        assertTrue(InverterManager.shouldDischargeNow(prices, at(18, 0), 50, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }

    public void testCurrentSegmentAboveThresholdButNotInTopN() {
        IntervalPrice[] prices = {
                seg(at(18, 0), 4),   // current, above threshold but cheapest
                seg(at(18, 15), 10),
                seg(at(18, 30), 9),
                seg(at(18, 45), 8),
        };
        // need 3 segments; top 3 are 18:15/18:30/18:45 -> current 18:00 not selected
        assertFalse(InverterManager.shouldDischargeNow(prices, at(18, 0), 50, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }

    public void testAllSegmentsBelowMinimalPrice() {
        IntervalPrice[] prices = {
                seg(at(18, 0), 2),
                seg(at(18, 15), 3),
                seg(at(18, 30), 2.5),
        };
        assertFalse(InverterManager.shouldDischargeNow(prices, at(18, 0), 50, 20, 5.0, CAPACITY_WH, DISCHARGE_W));
    }

    public void testSocAtOrBelowMinimal() {
        IntervalPrice[] prices = {seg(at(18, 0), 10)};
        assertFalse(InverterManager.shouldDischargeNow(prices, at(18, 0), 20, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
        assertFalse(InverterManager.shouldDischargeNow(prices, at(18, 0), 15, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }

    /** High-priced past and tomorrow segments must be ignored (horizon = remaining today). */
    public void testIgnoresPastAndTomorrowSegments() {
        IntervalPrice[] prices = {
                seg(at(17, 0), 100),        // fully in the past
                seg(tomorrowAt(9, 0), 100), // tomorrow
                seg(at(18, 0), 6),          // current
                seg(at(18, 15), 5),
        };
        // soc 30 -> 1000 Wh -> 1 segment needed; among today-future candidates 18:00 (6) is the top one
        assertTrue(InverterManager.shouldDischargeNow(prices, at(18, 0), 30, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }

    /** When fewer candidates exist than segments needed, every candidate (incl. current) is selected. */
    public void testFewerCandidatesThanNeeded() {
        IntervalPrice[] prices = {
                seg(at(18, 0), 6),  // current
                seg(at(18, 15), 7),
        };
        // soc 90 -> 7000 Wh -> ceil(7000/1475) = 5 needed, only 2 candidates -> current selected
        assertTrue(InverterManager.shouldDischargeNow(prices, at(18, 0), 90, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }

    public void testNullPrices() {
        assertFalse(InverterManager.shouldDischargeNow(null, at(18, 0), 50, 20, 1.0, CAPACITY_WH, DISCHARGE_W));
    }
}
