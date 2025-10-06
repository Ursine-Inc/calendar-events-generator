package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearTestCalendarCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;

    private static final Logger logger = LoggerFactory.getLogger(ClearTestCalendarCommand.class);

    public ClearTestCalendarCommand(CalendarService cal, String calendarId) {
        this.cal = cal;
        this.calendarId = calendarId;
    }

    @Override
    public void execute() throws Exception {
        logger.info("🧪 Clearing all events from test calendar...");
        cal.clearAllEvents(calendarId);
        logger.info("✅ All events cleared from test calendar.");
    }
}
