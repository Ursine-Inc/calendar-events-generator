package com.ursineenterprises.calendareventsgenerator.commands;

import com.ursineenterprises.calendareventsgenerator.services.CalendarService;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;
import com.ursineenterprises.calendareventsgenerator.commands.Command;

import java.util.List;

public class NormalRunCommand implements Command {
    private final CalendarService cal;
    private final String calendarId;
    private final List<ZoomEvent> events;

    public NormalRunCommand(CalendarService cal, String calendarId, List<ZoomEvent> events) {
        this.cal = cal;
        this.calendarId = calendarId;
        this.events = events;
    }

    @Override
    public void execute() throws Exception {
        for (ZoomEvent ev : events) {
            System.out.println("Processing: " + ev);
            var created = cal.insertWeeklyEvent(calendarId, ev);
            if (created != null) {
                System.out.println("✅ Created event id = " + created.getId());
            }
        }
    }
}
