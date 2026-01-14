package calendarapp;

import java.time.LocalDate;

public class RecurrenceRule {
    private final int eventId;               // root eventId (the first event in series)
    private final String interval;           // e.g. "1d", "2w", "1m"
    private final int times;                 // if > 0 => repeat this many occurrences
    private final LocalDate endDate;         // if not null => repeat until this date (inclusive)

    public RecurrenceRule(int eventId, String interval, int times, LocalDate endDate) {
        this.eventId = eventId;
        this.interval = interval;
        this.times = times;
        this.endDate = endDate;
    }

    public int getEventId() { return eventId; }
    public String getInterval() { return interval; }
    public int getTimes() { return times; }
    public LocalDate getEndDate() { return endDate; }

    public boolean usesTimes() { return times > 0; }
    public boolean usesEndDate() { return endDate != null; }
}
