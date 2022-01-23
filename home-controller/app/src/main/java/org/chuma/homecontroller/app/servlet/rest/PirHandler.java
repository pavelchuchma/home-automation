package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.PirStatus;

public class PirHandler extends AbstractRestHandler<PirStatus> {
    public PirHandler(Iterable<PirStatus> pirStatuses) {
        super("pir", "pir", pirStatuses, PirStatus::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, PirStatus ps, HttpServletRequest request) {
        jw.addAttribute("name", ps.getName());
        jw.addAttribute("active", (ps.isActive()) ? 1 : 0);
        long age = (ps.getLastActivate() != null) ? (new Date().getTime() - ps.getLastActivate().getTime()) / 1000 : -1;
        jw.addAttribute("age", age);
    }
}
