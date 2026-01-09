package calendarapp;

import java.time.LocalDateTime;

public class MainApp {
    public static void main(String[] args) {
        EventManager manager = new EventManager();

        // Create an event
        manager.createEvent("Meeting", "Team discussion",
                LocalDateTime.of(2025, 10, 5, 11, 0),
                LocalDateTime.of(2025, 10, 5, 12, 0));

        // View all events
        manager.viewAllEvents();

        // Update the event
        manager.updateEvent(1, "Updated Meeting", "Updated description",
                LocalDateTime.of(2025, 10, 5, 12, 0),
                LocalDateTime.of(2025, 10, 5, 13, 0));

        // View again
        manager.viewAllEvents();

        // Delete the event
        manager.deleteEvent(1);

        // View again
        manager.viewAllEvents();
    }
}


