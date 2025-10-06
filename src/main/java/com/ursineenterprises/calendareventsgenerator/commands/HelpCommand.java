package com.ursineenterprises.calendareventsgenerator.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelpCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);

        @Override
        public void execute() {
            logger.info("""
                    Google Calendar Event Generator — Usage
                    
                    java -jar calendar-events-generator-<VERSION>.jar [command]
                    
                    Commands:
                      (no args)             Run in normal mode and create events
                      --dry-run             Show which events would be created without modifying the calendar
                      --single-dry-run      Print a single example cURL command you can run manually
                      --clear-test-calendar Clear all events in the test calendar
                      --help                Show this help message
                    
                    Environment Variables:
                      GOOGLE_CALENDAR_ID   Google Calendar ID where events are created
                      GOOGLE_CREDENTIALS   Path to Google OAuth2 credentials file
                      EVENTS_FILE          Path to JSON file containing event data
                    
                    """);
        }
}
