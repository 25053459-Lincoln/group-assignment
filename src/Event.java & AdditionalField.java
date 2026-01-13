import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int eventId; // 
    private String title, description;
    private LocalDateTime startDateTime, endDateTime;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Event(int id, String t, String d, LocalDateTime s, LocalDateTime e) {
        this.eventId = id; this.title = t; this.description = d;
        this.startDateTime = s; this.endDateTime = e;
    }
    
    public String toCSV() { return eventId + "," + title + "," + description + "," + startDateTime + "," + endDateTime; }
    
    // Getters
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    
    @Override
    public String toString() {
        return String.format("ID: %d | %s | %s - %s", eventId, title, startDateTime, endDateTime);
    }
}

// Requirement 12: 独立文件存储额外字段 
class AdditionalField {
    int eventId;
    String location, category;
    public AdditionalField(int id, String loc, String cat) { this.eventId = id; this.location = loc; this.category = cat; }
    public String toCSV() { return eventId + "," + location + "," + category; }
}
