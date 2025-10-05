package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.CalendarEventsGenerator;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NormalRunCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;
    private final List<ZoomEvent> events;

    private static final Logger logger = LoggerFactory.getLogger(CalendarEventsGenerator.class);

    public NormalRunCommand(CalendarService cal, String calendarId, List<ZoomEvent> events) {
        this.cal = cal;
        this.calendarId = calendarId;
        this.events = events;
    }

    @Override
    public void execute() throws Exception {
        for (ZoomEvent ev : events) {
            logger.info("Processing: {}", ev);
            var created = cal.insertWeeklyEvent(calendarId, ev);
            if (created != null) {
                logger.info("✅ Created event id = {}", created.getId());
            }
        }
    }
}
