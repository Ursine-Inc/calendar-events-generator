package com.ursineenterprises.calendareventsgenerator;

import com.ursineenterprises.calendareventsgenerator.commands.Command;
import com.ursineenterprises.calendareventsgenerator.commands.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalendarEventsGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CalendarEventsGenerator.class);

    public static void main(String[] args) {
        try {
            Command cmd = CommandFactory.fromArgs(args);
            cmd.execute();
        } catch (Exception e) {
            logger.error("Failed to execute command", e);
            System.exit(1);
        }
    }
}
