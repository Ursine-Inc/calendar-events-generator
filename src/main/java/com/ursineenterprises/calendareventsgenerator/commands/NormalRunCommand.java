package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NormalRunCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;
    private final List<ZoomEvent> events;
    private final String timezone;

    private static final Logger logger = LoggerFactory.getLogger(NormalRunCommand.class);

    public NormalRunCommand(CalendarService cal, String calendarId, List<ZoomEvent> events, String timezone) {
        this.cal = cal;
        this.calendarId = calendarId;
        this.events = events;
        this.timezone = timezone;
    }

    @Override
    public void execute() throws Exception {
        logger.info("Posting to calendar {}", calendarId);
        for (ZoomEvent ev : events) {
            var created = cal.insertWeeklyEvent(calendarId, ev, timezone);
            if (created != null) {
                logger.info("✅ Created event: {}", created.getDescription());
            }
        }
    }
}
