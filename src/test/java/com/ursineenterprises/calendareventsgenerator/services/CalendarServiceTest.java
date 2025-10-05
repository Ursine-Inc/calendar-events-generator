package com.ursineenterprises.calendareventsgenerator.services;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private Calendar mockCalendar;

    @Mock
    private Calendar.Events mockEvents;

    @Mock
    private Calendar.Events.List mockEventsList;

    @Mock
    private Calendar.Events.Delete mockEventsDelete;

    private CalendarService calendarService;
    private final String testCalendarId = "test-calendar@gmail.com";

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() throws Exception {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        calendarService = new CalendarService() {
            @Override
            protected Calendar createCalendarService() {
                return mockCalendar;
            }
        };

        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.list(testCalendarId)).thenReturn(mockEventsList);
        when(mockEventsList.setMaxResults(anyInt())).thenReturn(mockEventsList);
        when(mockEventsList.setSingleEvents(Boolean.FALSE)).thenReturn(mockEventsList);
    }

    @Test
    void testClearAllEvents_EmptyCalendar() throws Exception {
        Events emptyEvents = new Events();
        emptyEvents.setItems(Collections.emptyList());
        when(mockEventsList.execute()).thenReturn(emptyEvents);

        calendarService.clearAllEvents(testCalendarId);

        verify(mockEvents, times(1)).list(testCalendarId);
        verify(mockEventsList, times(1)).execute();
        verify(mockEvents, never()).delete(any(), any());

        String output = outContent.toString();
        assertTrue(output.contains("[INFO] Fetching all events from calendar: " + testCalendarId));
        assertTrue(output.contains("[INFO] No events found to delete."));
    }

    @Test
    void testClearAllEvents_NullEventsList() throws Exception {
        Events nullEvents = new Events();
        nullEvents.setItems(null);
        when(mockEventsList.execute()).thenReturn(nullEvents);

        calendarService.clearAllEvents(testCalendarId);

        verify(mockEvents, times(1)).list(testCalendarId);
        verify(mockEventsList, times(1)).execute();
        verify(mockEvents, never()).delete(any(), any());

        String output = outContent.toString();
        assertTrue(output.contains("[INFO] No events found to delete."));
    }

    @Test
    void testClearAllEvents_SuccessfulDeletion() throws Exception {
        Event event1 = createMockEvent("event-1", "Meeting 1", null);
        Event event2 = createMockEvent("event-2", "Meeting 2", null);

        Events eventsResponse = new Events();
        eventsResponse.setItems(Arrays.asList(event1, event2));
        when(mockEventsList.execute()).thenReturn(eventsResponse);

        when(mockEvents.delete(testCalendarId, "event-1")).thenReturn(mockEventsDelete);
        when(mockEvents.delete(testCalendarId, "event-2")).thenReturn(mockEventsDelete);
        when(mockEventsDelete.execute()).thenReturn(null);

        // Act
        calendarService.clearAllEvents(testCalendarId);

        // Assert
        verify(mockEvents, times(1)).list(testCalendarId);
        verify(mockEvents, times(1)).delete(testCalendarId, "event-1");
        verify(mockEvents, times(1)).delete(testCalendarId, "event-2");
        verify(mockEventsDelete, times(2)).execute();

        String output = outContent.toString();
        assertTrue(output.contains("[INFO] Found 2 event(s) to delete."));
        assertTrue(output.contains("[INFO] Deleting event: Meeting 1 (ID: event-1)"));
        assertTrue(output.contains("[INFO] Deleting event: Meeting 2 (ID: event-2)"));
        assertTrue(output.contains("[SUCCESS] Deleted event 1/2"));
        assertTrue(output.contains("[SUCCESS] Deleted event 2/2"));
        assertTrue(output.contains("[INFO] ✅ Deletion complete: 2 deleted, 0 failed."));
    }

    @Test
    void testClearAllEvents_RecurringSeries() throws Exception {
        // Arrange
        Event recurringEvent1 = createMockEvent("event-1", "Recurring Meeting", "series-123");
        Event recurringEvent2 = createMockEvent("event-2", "Recurring Meeting", "series-123"); // Same series
        Event standaloneEvent = createMockEvent("event-3", "Standalone Meeting", null);

        Events eventsResponse = new Events();
        eventsResponse.setItems(Arrays.asList(recurringEvent1, recurringEvent2, standaloneEvent));
        when(mockEventsList.execute()).thenReturn(eventsResponse);

        when(mockEvents.delete(testCalendarId, "series-123")).thenReturn(mockEventsDelete);
        when(mockEvents.delete(testCalendarId, "event-3")).thenReturn(mockEventsDelete);
        when(mockEventsDelete.execute()).thenReturn(null);

        calendarService.clearAllEvents(testCalendarId);

        verify(mockEvents, times(1)).list(testCalendarId);
        verify(mockEvents, times(1)).delete(testCalendarId, "series-123");
        verify(mockEvents, times(1)).delete(testCalendarId, "event-3");
        verify(mockEvents, never()).delete(testCalendarId, "event-1");
        verify(mockEvents, never()).delete(testCalendarId, "event-2");
        verify(mockEventsDelete, times(2)).execute();

        String output = outContent.toString();
        assertTrue(output.contains("[INFO] Found 3 event(s) to delete."));
        assertTrue(output.contains("[INFO] ✅ Deletion complete: 2 deleted, 0 failed."));
    }

    @Test
    void testClearAllEvents_PartialFailure() throws Exception {
        Event event1 = createMockEvent("event-1", "Meeting 1", null);
        Event event2 = createMockEvent("event-2", "Meeting 2", null);

        Events eventsResponse = new Events();
        eventsResponse.setItems(Arrays.asList(event1, event2));
        when(mockEventsList.execute()).thenReturn(eventsResponse);

        when(mockEvents.delete(testCalendarId, "event-1")).thenReturn(mockEventsDelete);
        when(mockEvents.delete(testCalendarId, "event-2")).thenReturn(mockEventsDelete);
        when(mockEventsDelete.execute())
                .thenReturn(null) // First call succeeds
                .thenThrow(new RuntimeException("API Error")); // Second call fails

        RuntimeException exception = assertThrows(RuntimeException.class, () -> calendarService.clearAllEvents(testCalendarId));

        assertEquals("Failed to delete 1 event(s).", exception.getMessage());

        verify(mockEvents, times(2)).delete(any(), any());

        String output = outContent.toString();
        assertTrue(output.contains("[INFO] Found 2 event(s) to delete."));
        assertTrue(output.contains("[SUCCESS] Deleted event 1/2"));
        assertTrue(output.contains("[INFO] ✅ Deletion complete: 1 deleted, 1 failed."));

        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[ERROR] Failed to delete event: Meeting 2 - API Error"));
    }

    @Test
    void testClearAllEvents_ApiException() throws Exception {
        when(mockEventsList.execute()).thenThrow(new RuntimeException("Calendar API Error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> calendarService.clearAllEvents(testCalendarId));

        assertEquals("Calendar API Error", exception.getMessage());

        verify(mockEvents, times(1)).list(testCalendarId);
        verify(mockEvents, never()).delete(any(), any());
    }

    private Event createMockEvent(String id, String summary, String recurringEventId) {
        Event event = new Event();
        event.setId(id);
        event.setSummary(summary);
        if (recurringEventId != null) {
            event.setRecurringEventId(recurringEventId);
        }
        return event;
    }

    private void restoreSystemOut() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        restoreSystemOut();
    }
}
