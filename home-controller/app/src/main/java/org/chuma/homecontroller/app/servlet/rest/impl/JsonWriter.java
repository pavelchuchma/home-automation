package org.chuma.homecontroller.app.servlet.rest.impl;

import java.text.DecimalFormat;

public class JsonWriter implements AutoCloseable {
    private static final int MAX_DEPTH = 10;
    private final StringBuilder sb;
    private final boolean[] isFirstStack = new boolean[MAX_DEPTH];
    private final char[] endingChars = new char[MAX_DEPTH];
    private final boolean indent;
    private int stackPosition = -1;
    private DecimalFormat doubleFormat = new DecimalFormat("###.###");

    public JsonWriter(boolean indent) {
        this.indent = indent;
        this.sb = new StringBuilder();
    }

    private void startEntry(char startCharacter, char endCharacter) {
        isFirstStack[++stackPosition] = true;
        sb.append(startCharacter);
        endingChars[stackPosition] = endCharacter;
    }

    private void appendCommaIfNeeded() {
        if (stackPosition >= 0) {
            if (isFirstStack[stackPosition]) {
                isFirstStack[stackPosition] = false;
            } else {
                sb.append(",");
            }
            indent();
        }
    }

    private void indent() {
        if (!indent) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i <= stackPosition; i++) {
            sb.append("  ");
        }
    }

    public void close() {
        char endingChar = endingChars[stackPosition--];
        indent();
        sb.append(endingChar);
    }

    public JsonWriter startObject() {
        appendCommaIfNeeded();
        startEntry('{', '}');
        return this;
    }

    public JsonWriter addAttribute(String name, String value) {
        appendAttrName(name);
        sb.append("\"").append(value).append("\"");
        return this;
    }

    public JsonWriter addAttribute(String name, long value) {
        appendAttrName(name);
        sb.append(value);
        return this;
    }

    public JsonWriter addAttribute(String name, boolean value) {
        appendAttrName(name);
        sb.append(value);
        return this;
    }

    public JsonWriter addAttribute(String name, double value) {
        appendAttrName(name);
        sb.append(doubleFormat.format(value));
        return this;
    }

    public JsonWriter startArrayAttribute(String name) {
        appendAttrName(name);
        startEntry('[', ']');
        return this;
    }

    private void appendAttrName(String name) {
        appendCommaIfNeeded();
        sb.append("\"").append(name).append("\":");
    }

    public String toString() {
        return sb.toString();
    }
}
