package calendarapp;

import java.io.*;
import java.util.*;

public class AdditionalFieldManager {

    private static final String FILE = "additional.csv";

    // Add or update additional fields
    public void saveFields(int eventId, String location, String category, String attendees) {
        List<String[]> rows = readAll();
        boolean updated = false;

        for (String[] row : rows) {
            if (Integer.parseInt(row[0]) == eventId) {
                row[1] = location;
                row[2] = category;
                row[3] = attendees;
                updated = true;
                break;
            }
        }

        if (!updated) {
            rows.add(new String[]{
                    String.valueOf(eventId),
                    location,
                    category,
                    attendees
            });
        }

        writeAll(rows);
    }

    // Search by any additional field
    public void search(String keyword) {
        for (String[] row : readAll()) {
            if (row[1].contains(keyword) ||
                row[2].contains(keyword) ||
                row[3].contains(keyword)) {

                System.out.println(
                    "Event ID: " + row[0] +
                    ", Location: " + row[1] +
                    ", Category: " + row[2] +
                    ", Attendees: " + row[3]
                );
            }
        }
    }

    // Backup file
    public void backup(String backupFile) {
        copy(FILE, backupFile);
    }

    // Restore file
    public void restore(String backupFile) {
        copy(backupFile, FILE);
    }

    // ---------- helpers ----------

    private List<String[]> readAll() {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                rows.add(line.split(","));
            }
        } catch (IOException ignored) {}

        return rows;
    }

    private void writeAll(List<String[]> rows) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("eventId,location,category,attendees");
            for (String[] row : rows) {
                pw.println(String.join(",", row));
            }
        } catch (IOException ignored) {}
    }

    private void copy(String from, String to) {
        try (
            FileInputStream fis = new FileInputStream(from);
            FileOutputStream fos = new FileOutputStream(to)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException ignored) {}
    }
}
