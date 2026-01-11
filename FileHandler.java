import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    // 使用相对路径，符合文档 E.4 建议
    private static final String FILE_PATH = "data/event.csv"; 
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static List<Event> loadEvents() {
        List<Event> events = new ArrayList<>();
        File file = new File(FILE_PATH);
        
        if (!file.exists()) return events;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // 跳过 CSV 表头
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",\\s*");
                if (data.length >= 5) {
                    events.add(new Event(
                        Integer.parseInt(data[0]),
                        data[1],
                        data[2],
                        LocalDateTime.parse(data[3], FORMATTER),
                        LocalDateTime.parse(data[4], FORMATTER)
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件错误: " + e.getMessage());
        }
        return events;
    }
}
