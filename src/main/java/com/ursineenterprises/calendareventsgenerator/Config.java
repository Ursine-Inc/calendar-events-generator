package com.ursineenterprises.calendareventsgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
            } catch (IOException e) {
            System.out.println("Could not load application.properties file - " + e.getMessage());
        }

    }

    public static String get(String key, String envVarName) {
        String fromEnv = System.getenv(envVarName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return properties.getProperty(key);
    }

    public static int getInt(String key, String envVarName, int defaultValue) {
        String value = get(key, envVarName);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}