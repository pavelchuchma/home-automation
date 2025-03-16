package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.BeforeClass;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.SunCalculatorTest;
import org.chuma.homecontroller.extensions.external.utils.SolaxDataReportReader;

public class SolarPanelCalculatorTest {
    @BeforeClass
    public static void beforeClass() {
        SunCalculatorTest.beforeClass();
    }

    @Test
    public void testBasic() {
        int intervalMinutes = 15;
        double dailyPower = 0;
        SolarPanelCalculator calc = new SolarPanelCalculator(204.6, 33, 8200);
//        ZonedDateTime t0 = ZonedDateTime.of(2025, 3, 6, 0, 0, 0, 0, ZoneId.of("CET"));
//        ZonedDateTime t0 = ZonedDateTime.of(2024, 7, 30, 0, 0, 0, 0, ZoneId.of("CET"));
        ZonedDateTime t0 = ZonedDateTime.of(2024, 12, 1, 0, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime t = t0;
        do {
            double p = calc.calculateMaxPower(t);
            if (p > 0) {
                dailyPower += p / 1000 * (intervalMinutes / 60.0);
                System.out.printf("%tc: %d W%n", t, (int)p);
            }
            t = t.plusMinutes(intervalMinutes);
        } while (t.isBefore(t0.plusDays(1)));
        System.out.printf("Daily PV: %f kWH%n", dailyPower);
    }

    private void iterateThroughSolaxReport(String resourcePath, Consumer<SolaxDataReportReader.SolaxReportRow> consumer) throws IOException, InvalidFormatException {
        try (SolaxDataReportReader reader = new SolaxDataReportReader(resourcePath)) {

            Sheet sheet = reader.getSheet();
            Iterator<Row> rowIterator = sheet.rowIterator();
            rowIterator.next();
            rowIterator.next();
            SolaxDataReportReader.SolaxReportRow r;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                r = new SolaxDataReportReader.SolaxReportRow(row);
                consumer.accept(r);
            }
        }
    }

    static String[] reports = {"H34A15I7587435-2024-07-30.xlsx",
            "H34A15I7587435-2024-09-22.xlsx",
            "H34A15I7587435-2024-12-01.xlsx",
            "H34A15I7587435-2025-03-06.xlsx"};

    @Test
    public void testAgainstData() throws IOException, InvalidFormatException {
        SolarPanelCalculator calc = new SolarPanelCalculator(204.6, 33, 8200);

        for (String report : reports) {
            double[] dailyPvYield = {0};
            int[] positiveError = {0};
            int[] negativeError = {0};
            iterateThroughSolaxReport("/solax/" + report,
                    row -> {
                        ZonedDateTime time = row.getTime();
                        if (time.getMinute() < 5 || (time.getMinute() > 30 && time.getMinute() < 35)) {
                            int expPower = (int)calc.calculateMaxPower(time);
                            int realPower = (int)row.getPvPower();
                            if (expPower > realPower) {
                                positiveError[0] += expPower - realPower;
                            } else {
                                negativeError[0] += realPower - expPower;
                            }
                            if (expPower > 0 || realPower > 0) {
                                System.out.println(time + ": " + expPower + " x " + realPower + ", diff: " + (expPower - realPower));
                            }
                        }
                        dailyPvYield[0] = row.getDailyPvYield();
                    });
            System.out.printf("Daily PV: %.1f kWH, posError: %d, negError: %d%n", dailyPvYield[0], positiveError[0], negativeError[0]);
            System.out.println();
        }
    }
}