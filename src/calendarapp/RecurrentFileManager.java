package calendarapp;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class RecurrentFileManager {

    private static final String FILE_PATH = "data/recurrent.csv";

    public static Map<Integer, RecurrenceRule> readRules() {
        ensureFile();

        Map<Integer, RecurrenceRule> rules = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length != 4) continue;

                int eventId = Integer.parseInt(parts[0].trim());
                String interval = parts[1].trim();
                int times = Integer.parseInt(parts[2].trim());

                String endDateRaw = parts[3].trim();
                LocalDate endDate = (endDateRaw.equals("0") || endDateRaw.isEmpty())
                        ? null
                        : LocalDate.parse(endDateRaw);

                // rule uses either times OR endDate
                if (times > 0 && endDate != null) {
                    // if both set, prefer times (or you can reject)
                    endDate = null;
                }

                rules.put(eventId, new RecurrenceRule(eventId, interval, times, endDate));
            }
        } catch (IOException e) {
            System.out.println("Error reading recurrent.csv: " + e.getMessage());
        }
        return rules;
    }

    public static void saveAllRules(Collection<RecurrenceRule> rules) {
        ensureFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (RecurrenceRule r : rules) {
                bw.write(formatRule(r));
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving recurrent.csv: " + e.getMessage());
        }
    }

    public static void upsertRule(RecurrenceRule rule) {
        Map<Integer, RecurrenceRule> rules = readRules();
        rules.put(rule.getEventId(), rule);
        saveAllRules(rules.values());
    }

    public static void deleteRule(int eventId) {
        Map<Integer, RecurrenceRule> rules = readRules();
        rules.remove(eventId);
        saveAllRules(rules.values());
    }

    private static String formatRule(RecurrenceRule r) {
        String endDateOut = (r.getEndDate() == null) ? "0" : r.getEndDate().toString();
        int timesOut = r.getTimes(); // 0 means unused (per spec example)
        return r.getEventId() + "," + r.getInterval() + "," + timesOut + "," + endDateOut;
    }

    private static void ensureFile() {
        File f = new File(FILE_PATH);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!f.exists()) {
            try { f.createNewFile(); }
            catch (IOException e) { System.out.println("recurrent.csv creation failed: " + e.getMessage()); }
        }
    }
}
