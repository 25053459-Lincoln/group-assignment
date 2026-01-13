package calendarapp;

import java.time.LocalDateTime;

public class Event {
    private int eventId;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;

    public Event(int eventId, String title, String description,
                 LocalDateTime start, LocalDateTime end) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
    }
public Event(int id, String title,
             LocalDateTime start, LocalDateTime end) {
    this(id, title, "", start, end);
}

    public int getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }
    private int seriesId = -1;

public int getSeriesId() {
    return seriesId;
}

public void setSeriesId(int seriesId) {
    this.seriesId = seriesId;
}

}
