package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.time.ZonedDateTime;

import net.e175.klaus.solarpositioning.SolarPosition;
import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.extensions.external.SunCalculator;

public class SolarPanelCalculator {
    private final SunCalculator sunCalculator = SunCalculator.getInstance();
    /// Panel's azimuth angle in degrees (0° = North, 90° = East, etc.)
    private final double panelAzimuth;
    /// Panel's tilt angle in degrees (0° = horizontal, 90° = vertical)
    private final double panelTilt;
    private final double panelPeekPower;

    public SolarPanelCalculator(double panelAzimuth, double panelTilt, double panelPeekPower) {
        this.panelAzimuth = panelAzimuth;
        this.panelTilt = panelTilt;
        this.panelPeekPower = panelPeekPower;
    }

    private Vector vectorFromAngles(double azimuth, double zenithAngle) {
        double azRad = Math.toRadians(azimuth);
        double zenRad = Math.toRadians(zenithAngle);
        return new Vector(Math.cos(azRad), Math.sin(azRad), Math.tan(zenRad));
    }

    private double calculateVectorAngle(Vector a, Vector b) {
        double r1 = a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
        double aLen = Math.sqrt(a.x() * a.x() + a.y() * a.y() + a.z() * a.z());
        double bLen = Math.sqrt(b.x() * b.x() + b.y() * b.y() + b.z() * b.z());
        return Math.acos(r1 / (aLen * bLen));
    }

    public double calculateMaxPower(ZonedDateTime time) {
        SolarPosition pos = sunCalculator.calculateSolarPosition(time);
        if (pos.zenithAngle() > 89) {
            // sun is behind horizon
            return 0;
        }
        Vector panelVector = vectorFromAngles(panelAzimuth, 90 - panelTilt);
        Vector sunVector = vectorFromAngles(pos.azimuth(), 90 - pos.zenithAngle());
        double angle = calculateVectorAngle(panelVector, sunVector);
        double solarIntensity = calculateSolarIntensity(90 - pos.zenithAngle());
        double result = panelPeekPower * Math.cos(angle) * solarIntensity;
        return Math.max(result, 0);
    }

    public double calculateDailyYield(ZonedDateTime date) {
        // Get start of the day (midnight) in the same zone
        ZonedDateTime startOfDay = date.toLocalDate().atStartOfDay(date.getZone());
        // Get end of the day (midnight of the next day)
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        ZonedDateTime current = startOfDay;
        double segmentsPerHour = 4;
        double totalPower = 0;
        while (current.isBefore(endOfDay)) {
            totalPower += calculateMaxPower(current) / segmentsPerHour;
            current = current.plusMinutes((long)(60 / segmentsPerHour));
        }
        return totalPower / 1000;
    }

    public static double calculateSolarIntensity(double angleDegrees) {
//        final double k = 0.14;     // Extinction coefficient
        final double k = 0.075;     // Extinction coefficient
        Validate.inclusiveBetween(0, 90, angleDegrees);

        // Convert degrees to radians
        double angleRadians = Math.toRadians(angleDegrees);
        return Math.exp(-k / Math.sin(angleRadians));
    }

    record Vector(double x, double y, double z) {
    }
}
