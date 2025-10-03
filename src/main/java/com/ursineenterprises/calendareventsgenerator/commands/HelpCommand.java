package com.ursineenterprises.calendareventsgenerator.commands;

public class HelpCommand implements Command {
        @Override
        public void execute() {
            System.out.println("""
                    Google Calendar Event Generator — Usage
                    
                    java -jar calendar-events-generator--1.0.0.jar [command]
                    
                    Commands:
                      (no args)           Run in normal mode and create events
                      --dry-run           Show which events would be created without modifying the calendar
                      --single-dry-run    Print a single example cURL command you can run manually
                      --help              Show this help message
                    
                    Environment Variables:
                      GOOGLE_CALENDAR_ID   Google Calendar ID where events are created
                      GOOGLE_CREDENTIALS   Path to Google OAuth2 credentials file
                      EVENTS_FILE          Path to JSON file containing event data
                    
                    """);
        }
}
