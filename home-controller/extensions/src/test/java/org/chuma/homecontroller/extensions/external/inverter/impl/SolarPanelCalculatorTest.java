package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.github.cliftonlabs.json_simple.JsonException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.chuma.homecontroller.extensions.external.SunCalculatorTest;
import org.chuma.homecontroller.extensions.external.utils.LineGraph;
import org.chuma.homecontroller.extensions.external.utils.SolaxDataReportReader;

public class SolarPanelCalculatorTest {
    @BeforeAll
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

    @ParameterizedTest
    @ValueSource(strings = {
            "H34A15I7587435-2024-07-30.xlsx",
            "H34A15I7587435-2024-09-22.xlsx",
            "H34A15I7587435-2024-12-01.xlsx",
            "H34A15I7587435-2025-03-06.xlsx"
    })
    public void testAgainstDataClearDay(String report) throws IOException, InvalidFormatException {
        SolarPanelCalculator calc = new SolarPanelCalculator(202, 33, 8280);

        double[] dailyPvYield = {0};
        int[] positiveError = {0};
        int[] negativeError = {0};
        ZonedDateTime[] firstEntryTime = {null};
        List<LineGraph.DataPoint> points = new ArrayList<>();
        iterateThroughSolaxReport("/solax/" + report,
                row -> {
                    ZonedDateTime time = row.getTime();
                    if (time.getMinute() < 5 || (time.getMinute() >= 30 && time.getMinute() < 35)) {
                        int expPower = (int)calc.calculateMaxPower(time);
                        int realPower = (int)row.getPvPower();
                        if (expPower > realPower) {
                            positiveError[0] += (expPower - realPower) / 2;
                        } else {
                            negativeError[0] += (realPower - expPower) / 2;
                        }
                        if (expPower > 0 || realPower > 0) {
                            if (firstEntryTime[0] == null) {
                                firstEntryTime[0] = time;
                            }
                            System.out.println(time + ": " + expPower + " minutes " + realPower + ", diff: " + (expPower - realPower));
                            points.add(new LineGraph.DataPoint(
                                    (int)java.time.Duration.between(firstEntryTime[0], time).toMinutes(),
                                    expPower,
                                    realPower,
                                    0, 0, 0, 0, 0, 0));
                        }
                    }
                    dailyPvYield[0] = row.getDailyPvYield();
                });
        double calTotal = calc.calculateDailyYield(firstEntryTime[0]);
        System.out.printf("Daily PV: %.1f minutes %.1f kWH (diff %.1f %%), posError: %d, negError: %d%n",
                calTotal, dailyPvYield[0], (calTotal / dailyPvYield[0]) * 100 - 100, positiveError[0], negativeError[0]);
        System.out.println();
        LineGraph.drawGraphToFile(points, "out/" + report + ".png");
    }

    public static List<Path> getMatchingFiles(String directoryPath, String fileMask) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Provided path is not a directory: " + directoryPath);
        }

        // Convert mask to glob pattern
        String globPattern = "glob:" + fileMask;

        DirectoryStream.Filter<Path> filter = entry -> Files.isRegularFile(entry)
                && FileSystems.getDefault().getPathMatcher(globPattern).matches(entry.getFileName());

        List<Path> matchedFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            for (Path entry : stream) {
                matchedFiles.add(entry);
            }
        }

        matchedFiles.sort(Comparator.comparing(Path::getFileName));
        return matchedFiles;
    }

    WeatherForecast.Entry getWeatherForecastEntry(ZonedDateTime time, List<WeatherForecast.Forecast> forecasts) {
        WeatherForecast.Entry lastValid = null;
        for (WeatherForecast.Forecast forecast : forecasts) {
            WeatherForecast.Entry entry = forecast.entries().get(0);
            if (entry.time().isBefore(ChronoZonedDateTime.from(time))) {
                lastValid = entry;
            } else {
                break;
            }
        }
        if (lastValid != null && java.time.Duration.between(lastValid.time(), time).toMinutes() <= 60) {
            return lastValid;
        }
        throw new IllegalArgumentException("Could not find weather forecast for " + time);
    }

    public List<WeatherForecast.Forecast> deserializeWeatherForecast() throws IOException, JsonException {
        List<Path> files = getMatchingFiles("out/forecast-in", "2025*");
        List<WeatherForecast.Forecast> forecasts = new ArrayList<>();
        for (Path file : files) {
            try (Reader reader = new FileReader(file.toFile())) {
                forecasts.add(WeatherForecast.deserialize(reader));
            }
        }
        return forecasts;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "H34A15I7587435-2025-03-09.xlsx",
            "H34A15I7587435-2025-03-12.xlsx",
            "H34A15I7587435-2025-04-24.xlsx"
    })
    public void testAgainstDataCloudyDay(String report) throws IOException, InvalidFormatException, JsonException {
        SolarPanelCalculator calc = new SolarPanelCalculator(200, 33, 8280);
        List<WeatherForecast.Forecast> forecasts = deserializeWeatherForecast();

        double[] dailyPvYield = {0};
        int[] positiveError = {0};
        int[] negativeError = {0};
        ZonedDateTime[] firstEntryTime = {null};
        List<LineGraph.DataPoint> points = new ArrayList<>();
        iterateThroughSolaxReport("/solax/" + report,
                row -> {
                    ZonedDateTime time = row.getTime();
                    WeatherForecast.Entry fe = getWeatherForecastEntry(time, forecasts);
                    int expPower = (int)calc.calculateMaxPower(time);
                    int realPower = (int)row.getPvPower();
                    if (expPower > realPower) {
                        positiveError[0] += (expPower - realPower) / 12;
                    } else {
                        negativeError[0] += (realPower - expPower) / 12;
                    }
                    if (expPower > 0 || realPower > 0) {
                        if (firstEntryTime[0] == null) {
                            firstEntryTime[0] = time;
                        }
                        System.out.println(MessageFormat.format("{0}: {1} minutes {2}, diff: {3}, {4}:{5}/{6}/{7}", time, expPower, realPower, expPower - realPower,
                                fe.cloudAreaFraction(), fe.cloudAreaFractionLow(), fe.cloudAreaFractionMedium(), fe.cloudAreaFractionHigh()));
                        points.add(new LineGraph.DataPoint(
                                (int)java.time.Duration.between(firstEntryTime[0], time).toMinutes(),
                                expPower,
                                realPower,
                                fe.cloudAreaFraction(),
                                fe.cloudAreaFractionLow(),
                                fe.cloudAreaFractionMedium(),
                                fe.cloudAreaFractionHigh(),
                                fe.fogAreaFraction(),
                                fe.precipitationAmount()));
                    }
                    dailyPvYield[0] = row.getDailyPvYield();
                });
        double calTotal = calc.calculateDailyYield(firstEntryTime[0]);
        System.out.printf("Daily PV: %.1f minutes %.1f kWH (diff %.1f %%), posError: %d, negError: %d%n",
                calTotal, dailyPvYield[0], (calTotal / dailyPvYield[0]) * 100 - 100, positiveError[0], negativeError[0]);
        System.out.println();
        LineGraph.drawGraphToFile(points, "out/" + report + ".png");
    }
}