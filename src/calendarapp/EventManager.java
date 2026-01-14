package calendarapp;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class EventManager {

    private List<Event> events;
    private int nextEventId;

    // Optional (nice for assignment): store recurrence rule per SERIES root id
    // Uses: rootEventId -> RecurrenceRule
    private Map<Integer, RecurrenceRule> recurrenceRules;

    public EventManager() {
        events = FileManager.readEvents();          // load from data/event.csv
        nextEventId = getNextEventId();             // next ID
        try {
            recurrenceRules = RecurrentFileManager.readRules(); // load from data/recurrent.csv (if you created it)
        } catch (Exception ex) {
            recurrenceRules = new HashMap<>();
        }
    }

    /* =========================
       FEATURE 3: BASIC SEARCH
       ========================= */

    // Basic search by single date
    public List<Event> basicSearch(LocalDate date) {
        return basicSearch(date, date);
    }

    // Basic search by date range (inclusive)
    public List<Event> basicSearch(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw new IllegalArgumentException("start/end cannot be null");
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        LocalDate finalStart = start;
        LocalDate finalEnd = end;

        return events.stream()
                .filter(e -> {
                    LocalDate d = e.getStart().toLocalDate();
                    return (!d.isBefore(finalStart) && !d.isAfter(finalEnd));
                })
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // Keep your old method name so your GUI still works
    public List<Event> searchByDateRange(LocalDate start, LocalDate end) {
        return basicSearch(start, end);
    }

    /* =========================
       CREATE EVENTS
       ========================= */

    // Create a new single event (non-recurring)
    public void createEvent(String title, String desc, LocalDateTime start, LocalDateTime end) {
        int id = nextEventId++;
        Event e = new Event(id, title, desc, start, end);
        e.setRecurring(false);
        e.setSeriesId(0);
        events.add(e);
        saveAllEvents();
    }

    // Used by your GUI: it creates ALL occurrences and stores them in event.csv
    // Supports:
    // - DAILY / WEEKLY / MONTHLY (from your GUI)
    // - recurrenceCount = N times (NOT indefinite)
    public void addRecurringEvent(Event template) {
        // Convert GUI types to interval string (1d/1w/1m)
        String interval = toIntervalString(template.getRecurrenceType());

        // GUI uses recurrenceCount as "times"
        int times = template.getRecurrenceCount();
        if (times <= 0) throw new IllegalArgumentException("Repeat count must be > 0");

        createRecurringSeriesByTimes(
                template.getTitle(),
                template.getDescription(),
                template.getStart(),
                template.getEnd(),
                interval,
                times,
                template.getReminderMinutes()
        );
    }

    // Extra: recurring UNTIL end date (inclusive)
    public void addRecurringEventUntil(Event template, String interval, LocalDate endDateInclusive) {
        if (endDateInclusive == null) throw new IllegalArgumentException("endDate cannot be null");
        createRecurringSeriesByEndDate(
                template.getTitle(),
                template.getDescription(),
                template.getStart(),
                template.getEnd(),
                interval,
                endDateInclusive,
                template.getReminderMinutes()
        );
    }

    // Core: Create a recurring series repeating by TIMES
    // interval examples: "1d", "2w", "1m"
    private void createRecurringSeriesByTimes(String title, String desc,
                                              LocalDateTime start, LocalDateTime end,
                                              String interval, int times,
                                              int reminderMinutes) {

        Period step = RecurrenceUtil.parseIntervalToPeriod(interval);

        // root event is the first occurrence
        int rootId = nextEventId++;
        Event root = new Event(rootId, title, desc, start, end);
        root.setRecurring(true);
        root.setSeriesId(rootId);
        root.setRecurrenceType(interval);  // store interval string
        root.setRecurrenceCount(times);
        root.setReminderMinutes(reminderMinutes);
        events.add(root);

        LocalDateTime curStart = start;
        LocalDateTime curEnd = end;

        for (int i = 1; i < times; i++) {  // i=1 because root is occurrence #1
            curStart = curStart.plus(step);
            curEnd = curEnd.plus(step);

            // conflict check (optional) — if conflict skip or stop; here we BLOCK creation
            if (hasConflictExcludingEvent(curStart, curEnd, -1)) {
                // rollback series creation
                events.removeIf(e -> e.getSeriesId() == rootId);
                throw new IllegalStateException("Recurring event conflicts with another event at: " + curStart);
            }

            Event occ = new Event(nextEventId++, title, desc, curStart, curEnd);
            occ.setRecurring(true);
            occ.setSeriesId(rootId);
            occ.setRecurrenceType(interval);
            occ.setRecurrenceCount(times);
            occ.setReminderMinutes(reminderMinutes);
            events.add(occ);
        }

        // Save recurrence rule (optional for assignment)
        saveRecurrenceRule(rootId, interval, times, null);

        saveAllEvents();
    }

    // Core: Create a recurring series repeating UNTIL end date inclusive
    private void createRecurringSeriesByEndDate(String title, String desc,
                                                LocalDateTime start, LocalDateTime end,
                                                String interval, LocalDate endDateInclusive,
                                                int reminderMinutes) {

        Period step = RecurrenceUtil.parseIntervalToPeriod(interval);

        // root event
        int rootId = nextEventId++;
        Event root = new Event(rootId, title, desc, start, end);
        root.setRecurring(true);
        root.setSeriesId(rootId);
        root.setRecurrenceType(interval);
        root.setRecurrenceCount(0); // not used
        root.setReminderMinutes(reminderMinutes);
        events.add(root);

        LocalDateTime curStart = start;
        LocalDateTime curEnd = end;

        int safety = 0;
        while (true) {
            curStart = curStart.plus(step);
            curEnd = curEnd.plus(step);

            if (curStart.toLocalDate().isAfter(endDateInclusive)) break;

            if (hasConflictExcludingEvent(curStart, curEnd, -1)) {
                events.removeIf(e -> e.getSeriesId() == rootId);
                throw new IllegalStateException("Recurring event conflicts with another event at: " + curStart);
            }

            Event occ = new Event(nextEventId++, title, desc, curStart, curEnd);
            occ.setRecurring(true);
            occ.setSeriesId(rootId);
            occ.setRecurrenceType(interval);
            occ.setReminderMinutes(reminderMinutes);
            events.add(occ);

            if (++safety > 5000) break; // safety guard
        }

        // Save recurrence rule (optional for assignment)
        saveRecurrenceRule(rootId, interval, 0, endDateInclusive);

        saveAllEvents();
    }

    private String toIntervalString(String guiType) {
        if (guiType == null) return "1d";
        return switch (guiType.toUpperCase()) {
            case "DAILY" -> "1d";
            case "WEEKLY" -> "1w";
            case "MONTHLY" -> "1m";
            default -> {
                // Allow passing already like "2w", "3d"
                yield guiType.trim().toLowerCase();
            }
        };
    }

    private void saveRecurrenceRule(int rootEventId, String interval, int times, LocalDate endDate) {
        try {
            RecurrenceRule rule = new RecurrenceRule(rootEventId, interval, times, endDate);
            recurrenceRules.put(rootEventId, rule);
            RecurrentFileManager.upsertRule(rule);
        } catch (Exception ignored) {
            // If you didn't include recurrent.csv files, app still works without it.
        }
    }

    /* =========================
       UPDATE / DELETE
       ========================= */

    // Update a single event by ID (non-series safe)
    public void updateEvent(int id, String newTitle, String newDesc, LocalDateTime newStart, LocalDateTime newEnd) {
        for (Event e : events) {
            if (e.getEventId() == id) {
                if (hasConflictExcludingEvent(newStart, newEnd, id)) {
                    throw new IllegalStateException("This update conflicts with another event.");
                }
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

    // Update all events in a recurring series (simple approach)
    // Applies title/desc/time-of-day/duration to every occurrence, keeps each occurrence date.
    public void updateRecurringEvent(Event edited) {
        int seriesId = edited.getSeriesId();
        if (seriesId == 0) {
            // not a series, just update normally
            updateEvent(edited.getEventId(), edited.getTitle(), edited.getDescription(), edited.getStart(), edited.getEnd());
            return;
        }

        LocalTime newStartTime = edited.getStart().toLocalTime();
        long newDurationMinutes = Duration.between(edited.getStart(), edited.getEnd()).toMinutes();
        if (newDurationMinutes <= 0) newDurationMinutes = 60;

        // First pass: build proposed updated windows to detect conflicts
        for (Event e : events) {
            if (e.getSeriesId() == seriesId) {
                LocalDate d = e.getStart().toLocalDate();
                LocalDateTime ns = LocalDateTime.of(d, newStartTime);
                LocalDateTime ne = ns.plusMinutes(newDurationMinutes);

                // conflict check excluding events in same series
                if (hasConflictExcludingSeries(ns, ne, seriesId, e.getEventId())) {
                    throw new IllegalStateException("Series update conflicts with another event on " + d);
                }
            }
        }

        // Second pass: apply updates
        for (Event e : events) {
            if (e.getSeriesId() == seriesId) {
                LocalDate d = e.getStart().toLocalDate();
                LocalDateTime ns = LocalDateTime.of(d, newStartTime);
                LocalDateTime ne = ns.plusMinutes(newDurationMinutes);

                e.setTitle(edited.getTitle());
                e.setDescription(edited.getDescription());
                e.setStart(ns);
                e.setEnd(ne);
                e.setRecurring(true);
                e.setReminderMinutes(edited.getReminderMinutes());
                e.setRecurrenceType(edited.getRecurrenceType());
                e.setRecurrenceCount(edited.getRecurrenceCount());
            }
        }

        saveAllEvents();
    }

    private boolean hasConflictExcludingSeries(LocalDateTime newStart, LocalDateTime newEnd, int seriesId, int excludeId) {
        for (Event e : events) {
            if (e.getEventId() == excludeId) continue;
            if (e.getSeriesId() == seriesId) continue; // ignore same series
            if (newStart.isBefore(e.getEnd()) && newEnd.isAfter(e.getStart())) return true;
        }
        return false;
    }

    // Delete single event
    public void deleteEvent(int id) {
        events.removeIf(e -> e.getEventId() == id);
        saveAllEvents();
    }

    // Delete a recurring series (all occurrences)
    public void deleteRecurringEvent(Event event) {
        int seriesId = event.getSeriesId();
        if (seriesId != 0) {
            events.removeIf(e -> e.getSeriesId() == seriesId);
            // also remove recurrence rule if any
            try {
                recurrenceRules.remove(seriesId);
                RecurrentFileManager.deleteRule(seriesId);
            } catch (Exception ignored) {}
        } else {
            deleteEvent(event.getEventId());
        }
        saveAllEvents();
    }

    // Delete a single occurrence (simple implementation)
    public void deleteSingleOccurrence(Event event) {
        deleteEvent(event.getEventId());
    }

    /* =========================
       CONFLICT CHECKS
       ========================= */

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

    /* =========================
       BACKUP / RESTORE
       ========================= */

    // Backup events to CSV (same 10 fields you already use)
    public void backupEvents(String path) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            for (Event e : events) {
                writer.println(
                        e.getEventId() + "," +
                        safeCsv(e.getTitle()) + "," +
                        safeCsv(e.getDescription()) + "," +
                        e.getStart() + "," +
                        e.getEnd() + "," +
                        e.isRecurring() + "," +
                        safeCsv(e.getRecurrenceType()) + "," +
                        e.getRecurrenceCount() + "," +
                        e.getSeriesId() + "," +
                        e.getReminderMinutes()
                );
            }
        } catch (IOException ex) {
            System.out.println("Backup failed: " + ex.getMessage());
        }
    }

    // Restore events from CSV (same 10 fields)
    public void restoreEvents(String path) {
        events.clear();
        nextEventId = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // We wrote title/desc/type safely without commas, so split is OK
                String[] parts = line.split(",", -1);
                if (parts.length != 10) continue;

                int id = Integer.parseInt(parts[0].trim());
                String title = unsafecsv(parts[1]);
                String desc = unsafecsv(parts[2]);
                LocalDateTime start = LocalDateTime.parse(parts[3].trim());
                LocalDateTime end = LocalDateTime.parse(parts[4].trim());
                boolean recurring = Boolean.parseBoolean(parts[5].trim());
                String recurrenceType = unsafecsv(parts[6]);
                int recurrenceCount = Integer.parseInt(parts[7].trim());
                int seriesId = Integer.parseInt(parts[8].trim());
                int reminderMinutes = Integer.parseInt(parts[9].trim());

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

        saveAllEvents();
    }

    private String safeCsv(String s) {
        if (s == null) return "";
        // replace commas to keep CSV simple for your assignment
        return s.replace(",", " ");
    }

    private String unsafecsv(String s) {
        return s == null ? "" : s;
    }

    /* =========================
       STATS
       ========================= */

    public int getTotalEvents() { return events.size(); }

    public int getRecurringEventCount() {
        // count occurrences marked recurring
        return (int) events.stream().filter(Event::isRecurring).count();
    }

    public String getBusiestDay() {
        Map<DayOfWeek, Long> map = new HashMap<>();
        for (Event e : events) {
            map.put(e.getStart().getDayOfWeek(),
                    map.getOrDefault(e.getStart().getDayOfWeek(), 0L) + 1);
        }
        return map.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    /* =========================
       UTILITIES
       ========================= */

    public int getNextEventId() {
        return events.stream().mapToInt(Event::getEventId).max().orElse(0) + 1;
    }

    public List<Event> getEvents() { return events; }

    public void viewAllEvents() {
        System.out.println("=== All Events ===");
        for (Event e : events) {
            System.out.println(e.getEventId() + ": " + e.getTitle() +
                    " (" + e.getStart() + " to " + e.getEnd() + ")" +
                    (e.isRecurring() ? " [Series " + e.getSeriesId() + "]" : ""));
        }
    }

    private void saveAllEvents() {
        FileManager.saveEvents(events);
    }
}
