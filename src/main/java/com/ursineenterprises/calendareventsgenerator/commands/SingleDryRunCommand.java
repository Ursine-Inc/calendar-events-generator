package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.CalendarEventsGenerator;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleDryRunCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;
    private final ZoomEvent event;

    private static final Logger logger = LoggerFactory.getLogger(CalendarEventsGenerator.class);

    public SingleDryRunCommand(CalendarService cal, String calendarId, ZoomEvent event) {
        this.cal = cal;
        this.calendarId = calendarId;
        this.event = event;
    }

    @Override
    public void execute() throws Exception {
        String curl = cal.generateCurlPreview(calendarId, event);
        logger.info("🧪 SINGLE DRY RUN — Paste this into your shell to simulate:");
        logger.info(curl);
    }
}
