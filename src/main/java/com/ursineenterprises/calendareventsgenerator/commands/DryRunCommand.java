package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DryRunCommand implements Command {
    private final CalendarService calendarService;
    private final String calendarId;
    private final List<ZoomEvent> events;
    private static final Logger logger = LoggerFactory.getLogger(DryRunCommand.class);

    public DryRunCommand(CalendarService calendarService, String calendarId, List<ZoomEvent> events) {
        this.calendarService = calendarService;
        this.calendarId = calendarId;
        this.events = events;
    }

    @Override
    public void execute() throws Exception {
        for (ZoomEvent event : events) {
            logger.info("Processing: {}", event);
            boolean exists = calendarService.eventExists(calendarId, event);
            logger.info("🧪 DRY RUN: {} {} %n", exists ? "Already exists" : "Would create", event);
        }
    }
}
