package com.ursineenterprises.calendareventsgenerator.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import com.google.api.services.calendar.model.Events;
import com.ursineenterprises.calendareventsgenerator.CalendarEventsGenerator;
import com.ursineenterprises.calendareventsgenerator.Config;
import com.ursineenterprises.calendareventsgenerator.model.ZoomEvent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class CalendarService {
    private static final String APPLICATION_NAME = Config.get("application.name", "APPLICATION_NAME");
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final Calendar service;
    private final String timezone;

    public CalendarService() throws Exception {
        this.timezone = Config.get("default.timezone", "DEFAULT_TIMEZONE");
        this.service = createCalendarService();
    }

    private static Credential authorize() throws Exception {
        String credentialsPath = Config.get("credentials.file.path", "GOOGLE_CREDENTIALS_FILE_PATH");
        if (credentialsPath == null) {
            throw new IllegalStateException("Missing env var: GOOGLE_CREDENTIALS_FILE_PATH");
        }

        try (InputStream in = CalendarEventsGenerator.class.getResourceAsStream("/" + credentialsPath)) {
            if (in == null) {
                throw new RuntimeException(credentialsPath + " not found in classpath!");
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new java.io.InputStreamReader(in));

            String scopes = Config.get("google.scopes", "GOOGLE_API_SCOPES");
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    clientSecrets,
                    List.of(scopes)
            ).setAccessType("offline").build();

            int port = Config.getInt("oauth.port", "OAUTH_PORT", 8888);
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(port).build();
            return new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    private static Calendar createCalendarService() throws Exception {
        Credential credential = authorize();
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }

    public boolean eventExists(String calendarId, ZoomEvent ev) throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        LocalDate next = today.with(TemporalAdjusters.nextOrSame(ev.getDayOfWeek()));

        ZonedDateTime startZdt = ZonedDateTime.of(next, ev.getTime(), ZoneId.of(this.timezone));
        ZonedDateTime endZdt = startZdt.plusDays(1);

        return eventExists(calendarId, ev.getDescription(), startZdt, endZdt);
    }

    private boolean eventExists(String calendarId, String summary, ZonedDateTime start, ZonedDateTime end) throws Exception {
        Events events = service.events().list(calendarId)
                .setTimeMin(new DateTime(start.minusMinutes(5).toInstant().toEpochMilli()))
                .setTimeMax(new DateTime(end.plusMinutes(5).toInstant().toEpochMilli()))
                .setSingleEvents(true)
                .execute();

        if (events.getItems() == null) return false;

        return events.getItems().stream()
                .anyMatch(e -> summary.equalsIgnoreCase(e.getSummary()));
    }

    public Event insertWeeklyEvent(String calendarId, ZoomEvent ev) throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of(this.timezone));
        DayOfWeek desired = ev.getDayOfWeek();
        LocalDate next = today.with(TemporalAdjusters.nextOrSame(desired));

        ZonedDateTime startZdt = ZonedDateTime.of(next, ev.getTime(), ZoneId.of(this.timezone));
        ZonedDateTime endZdt = startZdt.plusHours(1);

        Event event = new Event();
        event.setSummary(ev.getDescription());
        String desc = "Zoom link: " + ev.getZoomUrl() + "\n\n" + ev.getDescription();
        event.setDescription(desc);

        List<String> recurrence = new ArrayList<>();
        recurrence.add("RRULE:FREQ=WEEKLY");
        event.setRecurrence(recurrence);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                .setTimeZone(this.timezone);
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                .setTimeZone(this.timezone);
        event.setEnd(end);

        return service.events().insert(calendarId, event).execute();
    }

    public String generateCurlPreview(String calendarId, ZoomEvent ev) throws Exception {
        Credential credential = authorize();

        String token = credential.getAccessToken();
        if (token == null) {
            throw new IllegalStateException("Could not retrieve OAuth access token.");
        }

        LocalDate today = LocalDate.now(ZoneId.of(this.timezone));
        LocalDate next = today.with(TemporalAdjusters.nextOrSame(ev.getDayOfWeek()));
        ZonedDateTime startZdt = ZonedDateTime.of(next, ev.getTime(), ZoneId.of(this.timezone));
        ZonedDateTime endZdt = startZdt.plusHours(1);

        String body = String.format("""
        {
          "summary": "%s",
          "description": "Zoom link: %s\\n\\n%s",
          "start": { "dateTime": "%s", "timeZone": "%s" },
          "end": { "dateTime": "%s", "timeZone": "%s" },
          "recurrence": ["RRULE:FREQ=WEEKLY"]
        }
        """,
                ev.getDescription(),
                ev.getZoomUrl(),
                ev.getDescription(),
                startZdt.toOffsetDateTime(),
                this.timezone,
                endZdt.toOffsetDateTime(),
                this.timezone);

        return String.format(
                "curl -X POST \\\n" +
                        "  'https://www.googleapis.com/calendar/v3/calendars/%s/events' \\\n" +
                        "  -H 'Authorization: Bearer %s' \\\n" +
                        "  -H 'Content-Type: application/json' \\\n" +
                        "  -d '%s'",
                calendarId,
                token,
                body.replace("\n", "").replace("\"", "\\\"")
        );
    }
}
