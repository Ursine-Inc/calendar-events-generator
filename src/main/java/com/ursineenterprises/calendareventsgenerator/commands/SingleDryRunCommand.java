package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.services.CalendarService;

public class SingleDryRunCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;
    private final ZoomEvent event;

    public SingleDryRunCommand(CalendarService cal, String calendarId, ZoomEvent event) {
        this.cal = cal;
        this.calendarId = calendarId;
        this.event = event;
    }

    @Override
    public void execute() throws Exception {
        String curl = cal.generateCurlPreview(calendarId, event);
        System.out.println("🧪 SINGLE DRY RUN — Paste this into your shell to simulate:");
        System.out.println(curl);
    }
}
