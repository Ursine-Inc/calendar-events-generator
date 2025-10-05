package com.ursineenterprises.calendareventsgenerator.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ursineenterprises.calendareventsgenerator.CalendarEventsGenerator;
import com.ursineenterprises.calendareventsgenerator.Config;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;

import java.io.InputStream;
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

        String calendarId = Config.get("google.calendar.id", "GOOGLE_CALENDAR_ID");
        if (calendarId == null) throw new IllegalStateException("Missing env var: GOOGLE_CALENDAR_ID");

        String eventsFilePath = Config.get("events.file.path", "EVENTS_FILE_PATH");
        if (eventsFilePath == null) throw new IllegalStateException("Missing env var: EVENTS_FILE");

        CalendarService cal = new CalendarService();
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream in = CalendarEventsGenerator.class.getResourceAsStream("/" + eventsFilePath)) {
            if (in == null) {
                throw new RuntimeException(eventsFilePath + " not found in classpath!");
            }

            List<Map<String, Object>> rawEvents = mapper.readValue(in, new TypeReference<>() {});
            List<ZoomEvent> events = rawEvents.stream().map(map -> new ZoomEvent(
                    DayOfWeek.valueOf(((String) map.get("dayOfWeek")).toUpperCase()),
                    LocalTime.parse(((String) map.get("time"))),
                    (String) map.get("zoomUrl"),
                    (String) map.get("description")
            )).toList();

            if (args.length > 0) {
                return switch (args[0]) {
                    case "--clear-test-calendar" -> new ClearTestCalendarCommand(cal, calendarId);
                    case "--dry-run" -> new DryRunCommand(cal, calendarId, events);
                    case "--single-dry-run" -> new SingleDryRunCommand(cal, calendarId, events.getFirst());
                    default -> new HelpCommand();
                };
            }

            return new NormalRunCommand(cal, calendarId, events);
        }
    }
}
