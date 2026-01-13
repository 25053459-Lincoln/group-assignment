package calendarapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.*;
import java.time.*;

public class EventManager {

    private List<Event> events;
    private int nextEventId; 
    public EventManager() {
        events = FileManager.readEvents(); // load events from CSV
    }

    // Create new event
    public void createEvent(String title, String desc,
                            LocalDateTime start, LocalDateTime end) {
        int id = events.size() + 1;
        Event e = new Event(id, title, desc, start, end);
        events.add(e);
        FileManager.saveEvent(e);
    }

    // View all events
    public void viewAllEvents() {
        System.out.println("=== All Events ===");
        for (Event e : events) {
            System.out.println(e.getEventId() + ": " + e.getTitle() +
                    " (" + e.getStart() + " to " + e.getEnd() + ")");
        }
    }

    // View events by date
    public void viewEventsByDate(int year, int month, int day) {
        System.out.println("=== Events on " + year + "-" + month + "-" + day + " ===");
        for (Event e : events) {
            if (e.getStart().getYear() == year &&
                e.getStart().getMonthValue() == month &&
                e.getStart().getDayOfMonth() == day) {
                System.out.println(e.getEventId() + ": " + e.getTitle() +
                    " (" + e.getStart() + " to " + e.getEnd() + ")");
            }
        }
    }

    // Update event by ID
    public void updateEvent(int id, String newTitle, String newDesc,
                            LocalDateTime newStart, LocalDateTime newEnd) {
        for (Event e : events) {
            if (e.getEventId() == id) {
                Event updated = new Event(id, newTitle, newDesc, newStart, newEnd);
                events.set(events.indexOf(e), updated);
                saveAllEvents();
                return;
            }
        }
        System.out.println("Event ID not found!");
    }

    // Delete event by ID
    public void deleteEvent(int id) {
        events.removeIf(e -> e.getEventId() == id);
        saveAllEvents();
    }

    // Save all events to CSV safely (used after update/delete)
    private void saveAllEvents() {
        File file = new File("data/event.csv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (Event e : events) {
                bw.write(
                    e.getEventId() + "," +
                    e.getTitle() + "," +
                    e.getDescription() + "," +
                    e.getStart() + "," +
                    e.getEnd() + "\n"
                );
            }
        } catch (IOException ex) {
            System.out.println("Error rewriting CSV: " + ex.getMessage());
        }
    }

    // Getter for all events (optional)
    public List<Event> getEvents() {
        return events;
    }
    public void addEvent(Event e) {
    events.add(e);
}

    
    public void backupEvents(String backupPath) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(backupPath))) {
        for (Event e : events) {
            writer.println(
                e.getEventId() + "," +
                e.getTitle() + "," +
                e.getDescription() + "," +
                e.getStart() + "," +
                e.getEnd()
            );
        }
        System.out.println("Backup has been completed to " + backupPath);
    } catch (IOException e) {
        System.out.println("Backup failed: " + e.getMessage());
    }
}
public void restoreEvents(String backupPath) {
    events.clear(); // delete existing events
    nextEventId = 1;

    try (BufferedReader reader = new BufferedReader(new FileReader(backupPath))) {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            int id = Integer.parseInt(parts[0]);
            String title = parts[1];
            String desc = parts[2];
            LocalDateTime start = LocalDateTime.parse(parts[3]);
            LocalDateTime end = LocalDateTime.parse(parts[4]);

            events.add(new Event(id, title, desc, start, end));
            nextEventId = Math.max(nextEventId, id + 1);
        }
        System.out.println("Restore completed from " + backupPath);
    } catch (IOException e) {
        System.out.println("Restore failed: " + e.getMessage());
    }
}
public void addRecurringEvent(
        String title,
        LocalDateTime start,
        Duration duration,
        int intervalDays,
        int count
) {
    int series = nextEventId;

    for (int i = 0; i < count; i++) {
       Event e = new Event(
    nextEventId++, 
    title, 
    "", // description placeholder
    start.plusDays((long) i * intervalDays),
    start.plusDays((long) i * intervalDays).plus(duration)
);

        e.setSeriesId(series);
        events.add(e);
    }
}
// Search events on a single date
public java.util.List<Event> searchByDate(LocalDate date) {
    java.util.List<Event> results = new java.util.ArrayList<>();
    for (Event e : events) {
        if (e.getStart().toLocalDate().equals(date)) {
            results.add(e);
        }
    }
    return results;
}

// Search events within a date range
public java.util.List<Event> searchByDateRange(LocalDate start, LocalDate end) {
    java.util.List<Event> results = new java.util.ArrayList<>();
    for (Event e : events) {
        LocalDate eventDate = e.getStart().toLocalDate();
        if (!eventDate.isBefore(start) && !eventDate.isAfter(end)) {
            results.add(e);
        }
    }
    return results;
}
public void addRecurringEvent(Event baseEvent) {
    events.add(baseEvent);

    if (!baseEvent.isRecurring()) return;

   LocalDateTime date = baseEvent.getStartDateTime();


    for (int i = 1; i < baseEvent.getRecurrenceCount(); i++) {
        switch (baseEvent.getRecurrenceType()) {
            case "DAILY":
                date = date.plusDays(1);
                break;
            case "WEEKLY":
                date = date.plusWeeks(1);
                break;
            case "MONTHLY":
                date = date.plusMonths(1);
                break;
        }

        Event copy = new Event(
                baseEvent.getTitle(),
                baseEvent.getDescription(),
                date
        );

        events.add(copy);
    }
}
public int getNextEventId() {
    int maxId = 0;
    for (Event e : events) { // assuming your list is called 'events'
        if (e.getEventId() > maxId) {
            maxId = e.getEventId();
        }
    }
    return maxId + 1;
}
public boolean hasConflict(LocalDateTime start, LocalDateTime end) {
    for (Event e : events) {
        if (start.isBefore(e.getEnd()) && end.isAfter(e.getStart())) {
            return true;
        }
    }
    return false;
}

// Total number of events
public int getTotalEvents() {
    return events.size();
}

// Busiest day of the week
public String getBusiestDay() {
    Map<java.time.DayOfWeek, Integer> countMap = new HashMap<>();

    for (Event e : events) {
        java.time.DayOfWeek day = e.getStart().getDayOfWeek();
        countMap.put(day, countMap.getOrDefault(day, 0) + 1);
    }

    java.time.DayOfWeek busiest = null;
    int max = 0;

    for (var entry : countMap.entrySet()) {
        if (entry.getValue() > max) {
            max = entry.getValue();
            busiest = entry.getKey();
        }
    }

    return busiest == null ? "N/A" : busiest.toString();
}

// Count recurring events
public int getRecurringEventCount() {
    int count = 0;
    for (Event e : events) {
        if (e.isRecurring()) count++;
    }
    return count;
}


}
