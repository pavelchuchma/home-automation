/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.chuma.homecontroller.extensions.external.robonect.client.model;

/**
 * Response holding the name of the mower used in the name command.
 *
 * @author Marco Meyer - Initial contribution
 */
public class Gps extends RobonectAnswer {

    private int satellites;
    private String latitude;
    private String longitude;

    public int getSatellites() {
        return satellites;
    }

    private static double parseGpsCoordinate(String coordinate) {
        coordinate = coordinate.trim().replace("°", " ");
        String[] parts = coordinate.split("\\s+");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid coordinate format: " + coordinate);
        }

        double degrees = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);

        return degrees + (minutes / 60.0);
    }

    public double getLatitude() {
        return parseGpsCoordinate(latitude);
    }

    public double getLongitude() {
        return parseGpsCoordinate(longitude);
    }

    @Override
    public String toString() {
        return "https://mapy.com/en/letecka?x=" + getLongitude() + "&y=" + getLatitude() + "&z=20";
    }
}
