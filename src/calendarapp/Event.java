package calendarapp;

import java.time.LocalDateTime;
import java.time.Duration;

public class Event {
    private int eventId;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean recurring;
    private String recurrenceType;
    private int recurrenceCount;
    private int seriesId;
    private int reminderMinutes; // minutes before event

    // Full constructor (for normal events)
    public Event(int eventId, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.recurring = false;
        this.recurrenceType = "DAILY";
        this.recurrenceCount = 0;
        this.seriesId = 0;
        this.reminderMinutes = 0;
    }

    // Minimal constructor (for recurring copies)
    public Event(String title, String description, LocalDateTime start) {
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = start.plusHours(1); // default duration
        this.recurring = false;
        this.recurrenceType = "DAILY";
        this.recurrenceCount = 0;
        this.seriesId = 0;
        this.reminderMinutes = 0;
    }

    // getters & setters
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public boolean isRecurring() { return recurring; }
    public String getRecurrenceType() { return recurrenceType; }
    public int getRecurrenceCount() { return recurrenceCount; }
    public int getSeriesId() { return seriesId; }
    public int getReminderMinutes() { return reminderMinutes; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public void setRecurrenceType(String type) { this.recurrenceType = type; }
    public void setRecurrenceCount(int count) { this.recurrenceCount = count; }
    public void setSeriesId(int seriesId) { this.seriesId = seriesId; }
    public void setReminderMinutes(int reminderMinutes) { this.reminderMinutes = reminderMinutes; }

    // Convenience for conflict checks and recurring updates
    public long getDurationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}
