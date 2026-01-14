package calendarapp;

import java.time.Period;

public class RecurrenceUtil {

    // interval format: <N><unit>, unit in {d,w,m}
    // examples: 1d, 2w, 1m
    public static Period parseIntervalToPeriod(String interval) {
        if (interval == null) throw new IllegalArgumentException("interval is null");
        String s = interval.trim().toLowerCase();

        if (s.length() < 2) throw new IllegalArgumentException("Invalid interval: " + interval);

        char unit = s.charAt(s.length() - 1);
        int n;
        try {
            n = Integer.parseInt(s.substring(0, s.length() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid interval number: " + interval);
        }

        if (n <= 0) throw new IllegalArgumentException("Interval must be > 0: " + interval);

        return switch (unit) {
            case 'd' -> Period.ofDays(n);
            case 'w' -> Period.ofWeeks(n);
            case 'm' -> Period.ofMonths(n);
            default -> throw new IllegalArgumentException("Invalid interval unit (use d/w/m): " + interval);
        };
    }
}
