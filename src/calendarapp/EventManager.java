package calendarapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class EventManager {

    private ArrayList<Event> events;

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
    public ArrayList<Event> getEvents() {
        return events;
    }
}
