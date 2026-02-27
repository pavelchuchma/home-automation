package org.chuma.homecontroller.extensions.external.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class SolaxDataReportReader implements Closeable {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Workbook workbook;
    private final Sheet sheet;

    public static class SolaxReportRow {
        final Row row;

        public SolaxReportRow(Row row) {
            this.row = row;
        }
        public ZonedDateTime getTime() {
            return ZonedDateTime.of(
                    java.time.LocalDateTime.parse(stripTrailingDot(row.getCell(0).getStringCellValue()), formatter),
                    ZoneId.systemDefault()
            );
        }

        private static String stripTrailingDot(String input) {
            if (input != null && input.endsWith(".")) {
                return input.substring(0, input.length() - 1);
            }
            return input;
        }

        public double getPvPower() {
            return row.getCell(35).getNumericCellValue();
        }

        public double getDailyPvYield() {
            return row.getCell(2).getNumericCellValue();
        }
    }
    // Constructor accepting path to XLSX file
    public SolaxDataReportReader(String resourcePath) throws IOException, InvalidFormatException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        assert inputStream != null;
        this.workbook = new XSSFWorkbook(inputStream);
        this.sheet = workbook.getSheetAt(0); // Get the first sheet
    }

    // Getter for sheet
    public Sheet getSheet() {
        return sheet;
    }

    @Override
    public void close() throws IOException {
        // Close the workbook correctly
        if (workbook != null) {
            workbook.close();
        }
    }
}
