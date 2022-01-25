package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;

public interface StatusHandler extends Handler {
    String getStatusJsonArrayName();

    void writeStatusJson(JsonWriter writer, HttpServletRequest request);
}
