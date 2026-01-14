package calendarapp;

import java.io.*;
import java.time.*;
import java.util.*;

public class EventManager {

    private List<Event> events;
    private int nextEventId;

    public EventManager() {
        events = FileManager.readEvents(); // load events from CSV if exists
        nextEventId = getNextEventId();
    }

    // Create a new single event
    public void createEvent(String title, String desc, LocalDateTime start, LocalDateTime end) {
        int id = nextEventId++;
        Event e = new Event(id, title, desc, start, end);
        events.add(e);
        FileManager.saveEvent(e);
    }

    // Add a recurring event (creates all occurrences)
    public void addRecurringEvent(Event event) {
        int seriesId = nextEventId; // first event ID becomes series ID
        LocalDateTime nextStart = event.getStart();
        LocalDateTime nextEnd = event.getEnd();

        for (int i = 0; i < event.getRecurrenceCount(); i++) {
            Event e = new Event(nextEventId++, event.getTitle(), event.getDescription(), nextStart, nextEnd);
            e.setRecurring(true);
            e.setRecurrenceType(event.getRecurrenceType());
            e.setRecurrenceCount(event.getRecurrenceCount());
            e.setSeriesId(seriesId);
            e.setReminderMinutes(event.getReminderMinutes());
            events.add(e);

            // advance start and end for next occurrence
            switch (event.getRecurrenceType()) {
                case "DAILY" -> { nextStart = nextStart.plusDays(1); nextEnd = nextEnd.plusDays(1); }
                case "WEEKLY" -> { nextStart = nextStart.plusWeeks(1); nextEnd = nextEnd.plusWeeks(1); }
                case "MONTHLY" -> { nextStart = nextStart.plusMonths(1); nextEnd = nextEnd.plusMonths(1); }
            }
        }
        saveAllEvents();
    }

    // Update a single event by ID
    public void updateEvent(int id, String newTitle, String newDesc, LocalDateTime newStart, LocalDateTime newEnd) {
        for (Event e : events) {
            if (e.getEventId() == id) {
                e.setTitle(newTitle);
                e.setDescription(newDesc);
                e.setStart(newStart);
                e.setEnd(newEnd);
                saveAllEvents();
                return;
            }
        }
        System.out.println("Event ID not found!");
    }

    // Update all events in a recurring series
    public void updateRecurringEvent(Event event) {
        int seriesId = event.getSeriesId();
        for (Event e : events) {
            if (e.getSeriesId() == seriesId) {
                e.setTitle(event.getTitle());
                e.setDescription(event.getDescription());
                e.setStart(event.getStart());
                e.setEnd(event.getEnd());
                e.setRecurring(event.isRecurring());
                e.setRecurrenceType(event.getRecurrenceType());
                e.setRecurrenceCount(event.getRecurrenceCount());
                e.setReminderMinutes(event.getReminderMinutes());
            }
        }
        saveAllEvents();
    }

    // Delete single event
    public void deleteEvent(int id) {
        events.removeIf(e -> e.getEventId() == id);
        saveAllEvents();
    }

    // Delete a recurring series
    public void deleteRecurringEvent(Event event) {
        if (event.getSeriesId() != 0) {
            events.removeIf(e -> e.getSeriesId() == event.getSeriesId());
        } else {
            deleteEvent(event.getEventId());
        }
        saveAllEvents();
    }

    // Delete a single occurrence
    public void deleteSingleOccurrence(Event event) {
        deleteEvent(event.getEventId());
    }

    // Conflict check excluding a specific event (for updates)
    public boolean hasConflictExcludingEvent(LocalDateTime newStart, LocalDateTime newEnd, int excludeId) {
        for (Event e : events) {
            if (e.getEventId() == excludeId) continue;
            if (newStart.isBefore(e.getEnd()) && newEnd.isAfter(e.getStart())) return true;
        }
        return false;
    }

    // Conflict check for new events
    public boolean hasConflict(LocalDateTime newStart, LocalDateTime newEnd) {
        return hasConflictExcludingEvent(newStart, newEnd, -1);
    }

    // Search events by date range
    public List<Event> searchByDateRange(LocalDate start, LocalDate end) {
        List<Event> results = new ArrayList<>();
        for (Event e : events) {
            LocalDate eventDate = e.getStart().toLocalDate();
            if (!eventDate.isBefore(start) && !eventDate.isAfter(end)) results.add(e);
        }
        return results;
    }

    // Backup events to a CSV file
    public void backupEvents(String path) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            for (Event e : events) {
                writer.println(e.getEventId() + "," + e.getTitle() + "," + e.getDescription() + "," +
                               e.getStart() + "," + e.getEnd() + "," + e.isRecurring() + "," +
                               e.getRecurrenceType() + "," + e.getRecurrenceCount() + "," +
                               e.getSeriesId() + "," + e.getReminderMinutes());
            }
        } catch (IOException ex) {
            System.out.println("Backup failed: " + ex.getMessage());
        }
    }

    // Restore events from a CSV file
    public void restoreEvents(String path) {
        events.clear();
        nextEventId = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String desc = parts[2];
                LocalDateTime start = LocalDateTime.parse(parts[3]);
                LocalDateTime end = LocalDateTime.parse(parts[4]);
                boolean recurring = Boolean.parseBoolean(parts[5]);
                String recurrenceType = parts[6];
                int recurrenceCount = Integer.parseInt(parts[7]);
                int seriesId = Integer.parseInt(parts[8]);
                int reminderMinutes = Integer.parseInt(parts[9]);

                Event e = new Event(id, title, desc, start, end);
                e.setRecurring(recurring);
                e.setRecurrenceType(recurrenceType);
                e.setRecurrenceCount(recurrenceCount);
                e.setSeriesId(seriesId);
                e.setReminderMinutes(reminderMinutes);
                events.add(e);
                nextEventId = Math.max(nextEventId, id + 1);
            }
        } catch (IOException ex) {
            System.out.println("Restore failed: " + ex.getMessage());
        }
    }

    // Stats
    public int getTotalEvents() { return events.size(); }
    public int getRecurringEventCount() { return (int) events.stream().filter(Event::isRecurring).count(); }
    public String getBusiestDay() {
        Map<DayOfWeek, Long> map = new HashMap<>();
        for (Event e : events) {
            map.put(e.getStart().getDayOfWeek(), map.getOrDefault(e.getStart().getDayOfWeek(), 0L) + 1);
        }
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> e.getKey().toString()).orElse("N/A");
    }

    // Utilities
    public int getNextEventId() {
        return events.stream().mapToInt(Event::getEventId).max().orElse(0) + 1;
    }

    public List<Event> getEvents() { return events; }

    // Save all events
    private void saveAllEvents() { FileManager.saveEvents(events); }

    
public void viewAllEvents() {
    System.out.println("=== All Events ===");
    for (Event e : events) {
        System.out.println(e.getEventId() + ": " + e.getTitle() +
                " (" + e.getStart() + " to " + e.getEnd() + ")");
    }
}

}
