package com.ursineenterprises.calendareventsgenerator.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ursineenterprises.calendareventsgenerator.Config;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class CommandFactory {
    public static Command fromArgs(String[] args) throws Exception {
        // Help should be available even if env vars are missing
        if (args.length > 0 && args[0].equals("--help")) {
            return new HelpCommand();
        }

        String calendarId = Config.get(null, "GOOGLE_CALENDAR_ID");
        if (calendarId == null) throw new IllegalStateException("Missing env var: GOOGLE_CALENDAR_ID");

        String eventsFilePath = Config.get(null, "EVENTS_FILE");
        if (eventsFilePath == null) throw new IllegalStateException("Missing env var: EVENTS_FILE");

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> rawEvents = mapper.readValue(new File(eventsFilePath), new TypeReference<>() {});

        List<ZoomEvent> events = rawEvents.stream().map(map -> new ZoomEvent(
                DayOfWeek.valueOf(((String) map.get("dayOfWeek")).toUpperCase()),
                LocalTime.parse(((String) map.get("time"))),
                (String) map.get("zoomUrl"),
                (String) map.get("description")
        )).toList();

        CalendarService cal = new CalendarService();

        if (args.length > 0) {
            return switch (args[0]) {
                case "--dry-run" -> new DryRunCommand(cal, calendarId, events);
                case "--single-dry-run" -> new SingleDryRunCommand(cal, calendarId, (ZoomEvent) events);
                default -> new HelpCommand();
            };
        }

        return new NormalRunCommand(cal, calendarId, events);
    }
}
