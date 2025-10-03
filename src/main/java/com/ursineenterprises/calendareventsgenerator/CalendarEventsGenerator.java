package com.ursineenterprises.calendareventsgenerator;

import com.ursineenterprises.calendareventsgenerator.commands.Command;
import com.ursineenterprises.calendareventsgenerator.commands.CommandFactory;

public class CalendarEventsGenerator {
    public static void main(String[] args) {
        try {
            Command cmd = CommandFactory.fromArgs(args);
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
