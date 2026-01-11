import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String FILE_PATH = "data/event.csv";
    private static final String BACKUP_PATH = "data/backup_event.csv";

    // Step 5: 保存单个事件到 CSV
    public static void saveEvent(Event event) {
        ensureDirectoryExists();
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_PATH, true))) {
            // 如果文件是空的，先写表头
            File file = new File(FILE_PATH);
            if (file.length() == 0) {
                out.println("eventId, title, description, startDateTime, endDateTime");
            }
            out.println(event.toCSV());
        } catch (IOException e) {
            System.err.println("Error saving event: " + e.getMessage());
        }
    }

    // Step 5: 查看所有事件 (Listing)
    public static List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return events;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // 跳过表头
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",\\s*");
                if (parts.length >= 5) {
                    events.add(new Event(
                        Integer.parseInt(parts[0]), parts[1], parts[2],
                        LocalDateTime.parse(parts[3]), LocalDateTime.parse(parts[4])
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading events: " + e.getMessage());
        }
        return events;
    }

    // Step 11: 备份功能 (Copy event.csv to backup_event.csv)
    public static void backupData() {
        try {
            Files.copy(Paths.get(FILE_PATH), Paths.get(BACKUP_PATH), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup successful: " + BACKUP_PATH);
        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
        }
    }

    // Step 12: 恢复功能 (Restore from backup)
    public static void restoreData() {
        try {
            Files.copy(Paths.get(BACKUP_PATH), Paths.get(FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Restore successful from backup!");
        } catch (IOException e) {
            System.err.println("Restore failed: " + e.getMessage());
        }
    }

    private static void ensureDirectoryExists() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
    }
}
