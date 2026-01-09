package calendarapp;



import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class FileManager {

    private static final String FILE_PATH = "data/event.csv"; // relative path

    // Read all events from CSV
    public static ArrayList<Event> readEvents() {
        ArrayList<Event> events = new ArrayList<>();

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile(); // create CSV if missing
            } catch (IOException e) {
                System.out.println("Error creating CSV file: " + e.getMessage());
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // skip empty lines

                String[] parts = line.split(",");
                if (parts.length != 5) continue; // skip bad lines

                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String desc = parts[2];
                LocalDateTime start = LocalDateTime.parse(parts[3]);
                LocalDateTime end = LocalDateTime.parse(parts[4]);

                events.add(new Event(id, title, desc, start, end));
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        return events;
    }

    // Save one event (append)
    public static void saveEvent(Event e) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(
                e.getEventId() + "," +
                e.getTitle() + "," +
                e.getDescription() + "," +
                e.getStart() + "," +
                e.getEnd() + "\n"
            );
        } catch (IOException ex) {
            System.out.println("Error writing to file: " + ex.getMessage());
        }
    }
}
