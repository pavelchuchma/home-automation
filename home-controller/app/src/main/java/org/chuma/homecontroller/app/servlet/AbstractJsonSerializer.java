package org.chuma.homecontroller.app.servlet;

abstract class AbstractJsonSerializer {
    static void appendNameValue(StringBuffer b, String name, String value) {
        b.append("\"").append(name).append("\":\"").append(value).append("\"");
    }

    static void appendNameValue(StringBuffer b, String name, boolean value) {
        b.append("\"").append(name).append("\":").append(Boolean.toString(value));
    }

    static void appendNameValue(StringBuffer b, String name, int value) {
        b.append("\"").append(name).append("\":").append(Integer.toString(value));
    }

    static void appendNameValue(StringBuffer b, String name, double value) {
        b.append("\"").append(name).append("\":").append(String.format("%.2f", value));
    }
}