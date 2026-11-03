package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Room;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Status;
import org.chuma.homecontroller.extensions.external.floorheater.HeatingRange;

/**
 * Floor heating rooms: {@code /rest/floorHeater/status} lists them with their heating ranges, {@code /action}
 * changes one room. Unlike the other handlers the action runs synchronously, so that a rejected change (an
 * invalid range, no prices for the plan) comes back as 400 with the message for the dialog.
 */
public class FloorHeaterHandler extends AbstractRestHandler<Room> {
    private static final Logger log = LoggerFactory.getLogger(FloorHeaterHandler.class);
    private final FloorHeaterManager manager;

    public FloorHeaterHandler(FloorHeaterManager manager) {
        super("floorHeater", "floorHeater", manager.getRooms(), Room::id);
        this.manager = manager;
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        if (!target.startsWith(actionPath)) {
            super.handle(target, request, response);
            return;
        }
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        try {
            Map<String, String[]> params = request.getParameterMap();
            processAction(getItemById(getMandatoryStringParam(params, "id")), params);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (RuntimeException e) {
            log.warn("Floor heater action '{}' failed: {}", request.getQueryString(), e.toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=utf-8");
            response.getWriter().print(e.getMessage() != null ? e.getMessage() : e.toString());
        }
        request.setHandled(true);
    }

    @Override
    public void writeStatusJson(JsonWriter writer, HttpServletRequest request) {
        String id = getStringParam(request.getParameterMap(), "id");
        List<Status> statuses = (id != null) ? List.of(manager.getStatus(getItemById(id).id())) : manager.getStatuses();
        try (JsonWriter arrayWriter = writer.startArrayAttribute(statusJsonArrayName)) {
            for (Status s : statuses) {
                try (JsonWriter objectWriter = arrayWriter.startObject()) {
                    writeIdImpl(s.room().id(), objectWriter);
                    writeStatus(objectWriter, s);
                }
            }
        }
    }

    void writeStatus(JsonWriter jw, Status s) {
        jw.addAttribute("name", s.room().name());
        jw.addAttribute("val", s.on() ? 1 : 0);
        jw.addAttribute("powerW", s.room().powerW());
        jw.addAttribute("minutes", s.minutes());
        jw.addAttribute("scheduleUntil", s.scheduleUntil() != null ? millis(s.scheduleUntil()) : 0);
        jw.addAttribute("canPlan", s.canPlan());
        jw.addAttribute("target", manager.getTargetMinutes());
        jw.addAttribute("laterBonus", manager.getLaterBonusHaler());
        jw.addAttribute("manualMinutes", FloorHeaterManager.MANUAL_MINUTES);
        try (JsonWriter ranges = jw.startArrayAttribute("ranges")) {
            for (HeatingRange r : s.ranges()) {
                try (JsonWriter range = ranges.startObject()) {
                    range.addAttribute("from", millis(r.from()));
                    range.addAttribute("to", millis(r.to()));
                    range.addAttribute("kind", r.kind().name().toLowerCase());
                }
            }
        }
    }

    /**
     * All parameters are optional and applied in this order: switch (on|off), schedule (on|off), minutes,
     * ranges (the serialized list), target (minutes of the day), laterBonus (halers per kWh and hour).
     */
    @Override
    void processAction(Room room, Map<String, String[]> params) {
        String sw = getStringParam(params, "switch");
        if (sw != null) {
            if ("on".equals(sw)) {
                manager.switchOn(room.id());
            } else {
                manager.switchOff(room.id());
            }
        }
        String schedule = getStringParam(params, "schedule");
        if (schedule != null) {
            manager.setSchedule(room.id(), "on".equals(schedule));
        }
        String minutes = getStringParam(params, "minutes");
        if (minutes != null) {
            manager.setMinutes(room.id(), Integer.parseInt(minutes));
        }
        String ranges = getStringParam(params, "ranges");
        if (ranges != null) {
            manager.setRanges(room.id(), ranges);
        }
        String target = getStringParam(params, "target");
        if (target != null) {
            manager.setTargetMinutes(Integer.parseInt(target));
        }
        String bonus = getStringParam(params, "laterBonus");
        if (bonus != null) {
            manager.setLaterBonusHaler(Integer.parseInt(bonus));
        }
    }

    private static long millis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
