package calendarapp;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final String FILE_PATH = "data/event.csv";

    // Read all events
    public static List<Event> readEvents() {
        List<Event> events = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try { file.createNewFile(); } 
            catch (IOException e) { System.out.println("CSV creation failed: " + e.getMessage()); }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != 10) continue; // make sure all fields are present

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
            }
        } catch (IOException e) { System.out.println("Error reading CSV: " + e.getMessage()); }

        return events;
    }

    // Append one event
    public static void saveEvent(Event e) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(formatEvent(e) + "\n");
        } catch (IOException ex) { System.out.println("Error saving event: " + ex.getMessage()); }
    }

    // Save all events (overwrite)
    public static void saveEvents(List<Event> events) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Event e : events) {
                bw.write(formatEvent(e) + "\n");
            }
        } catch (IOException ex) { System.out.println("Error saving events: " + ex.getMessage()); }
    }

    // Helper
    private static String formatEvent(Event e) {
        return e.getEventId() + "," + e.getTitle() + "," + e.getDescription() + "," +
               e.getStart() + "," + e.getEnd() + "," + e.isRecurring() + "," +
               e.getRecurrenceType() + "," + e.getRecurrenceCount() + "," +
               e.getSeriesId() + "," + e.getReminderMinutes();
    }
}
