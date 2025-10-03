package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;

import java.util.List;

public class DryRunCommand implements Command {
    private final CalendarService calendarService;
    private final String calendarId;
    private final List<ZoomEvent> events;

    public DryRunCommand(CalendarService calendarService, String calendarId, List<ZoomEvent> events) {
        this.calendarService = calendarService;
        this.calendarId = calendarId;
        this.events = events;
    }

    @Override
    public void execute() throws Exception {
        for (ZoomEvent event : events) {
            System.out.println("Processing: " + event);
            boolean exists = calendarService.eventExists(calendarId, event);
            System.out.printf("🧪 DRY RUN: %s %s%n", exists ? "Already exists" : "Would create", event);
        }
    }
}
