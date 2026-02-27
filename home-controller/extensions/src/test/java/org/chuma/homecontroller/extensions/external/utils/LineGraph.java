package org.chuma.homecontroller.extensions.external.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class LineGraph {

    public record DataPoint(
            int minutes, // minutes since start
            int compPower,
            int realPower,
            double cloudAreaFraction,
            double cloudAreaFractionLow,
            double cloudAreaFractionMedium,
            double cloudAreaFractionHigh,
            double fogAreaFraction,
            double precipitationAmount
    ) {
    }

    private static void drawGraph(List<DataPoint> dataPoints, BufferedImage image) {
        int padding = 50;
        int labelPadding = 25;
        Graphics2D g = image.createGraphics();
        int width = image.getWidth();
        int height = image.getHeight();
        Color[] colors = new Color[]{Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.PINK, Color.YELLOW, Color.GRAY};
        String[] labels = new String[]{"comp", "real", "cloud", "cloud low", "cloud medium", "cloud high", "fog", "precipitation"};

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int graphWidth = width - 2 * padding - labelPadding;
        int graphHeight = height - 2 * padding - labelPadding;

        // Find max Y
        int maxY = dataPoints.stream().mapToInt(p -> Math.max(p.compPower, p.realPower)).max().orElse(1);

        // Draw axis
        g.setColor(Color.BLACK);
        g.drawLine(padding + labelPadding, height - padding, padding + labelPadding, padding); // Y Axis
        g.drawLine(padding + labelPadding, height - padding, width - padding, height - padding); // X Axis

        // Plot points and lines
        int prevX = -1;

        int[][] yValues = new int[colors.length][];
        for (int i = 0; i < colors.length; i++) {
            yValues[i] = new int[dataPoints.size()];
        }

        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint p = dataPoints.get(i);
            yValues[0][i] = height - padding - (int)((double)p.compPower / maxY * graphHeight);
            yValues[1][i] = height - padding - (int)((double)p.realPower / maxY * graphHeight);
            yValues[2][i] = height - padding - (int)(p.cloudAreaFraction() / 100 * graphHeight) + 1;
            yValues[3][i] = height - padding - (int)(p.cloudAreaFractionLow() / 100 * graphHeight) + 2;
            yValues[4][i] = height - padding - (int)(p.cloudAreaFractionMedium() / 100 * graphHeight) + 3;
            yValues[5][i] = height - padding - (int)(p.cloudAreaFractionHigh() / 100 * graphHeight) + 4;
            yValues[6][i] = height - padding - (int)(p.fogAreaFraction() / 100 * graphHeight) + 5;
            yValues[7][i] = height - padding - (int)(Math.min(1, p.precipitationAmount()/2) * graphHeight) + 6;
        }

        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint p = dataPoints.get(i);
            int x = padding + labelPadding + (int)((double)p.minutes / dataPoints.get(dataPoints.size() - 1).minutes * graphWidth);

            if (i != 0) {
                for (int c = 0; c < yValues.length; c++) {
                    g.setColor(colors[c]);
                    g.drawLine(prevX, yValues[c][i - 1], x, yValues[c][i]);
                }
            }
            prevX = x;
        }

        for (int c = 0; c < yValues.length; c++) {
            g.setColor(colors[c]);
            g.drawString(labels[c], width - padding - 70, padding + 15 * c);
        }
    }

    public static void drawGraphToFile(List<DataPoint> dataPoints, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_ARGB);
        LineGraph.drawGraph(dataPoints, image);
        ImageIO.write(image, "png", new File(fileName));
    }
}
