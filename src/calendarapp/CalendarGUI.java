package calendarapp;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Duration;
import java.util.List;

public class CalendarGUI extends JFrame {

    private JLabel monthLabel;
    private JPanel calendarPanel;
    private YearMonth currentMonth;
    private final EventManager manager;

    // Constructor
    public CalendarGUI(EventManager manager) {
        this.manager = manager;
        currentMonth = YearMonth.now();

        setTitle("Calendar App");
        setSize(700, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== Top panel =====
        JPanel topPanel = new JPanel();

        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setPreferredSize(new Dimension(160, 25));

        JButton searchBtn = new JButton("Search");
        JButton listViewBtn = new JButton("List View");
        JButton statsBtn = new JButton("Statistics");
        JButton backupBtn = new JButton("Backup");
        JButton restoreBtn = new JButton("Restore");

        topPanel.add(prevBtn);
        topPanel.add(monthLabel);
        topPanel.add(nextBtn);

        topPanel.add(searchBtn);
        topPanel.add(listViewBtn);
        topPanel.add(statsBtn);
        topPanel.add(backupBtn);
        topPanel.add(restoreBtn);

        add(topPanel, BorderLayout.NORTH);

        // ===== Calendar panel =====
        calendarPanel = new JPanel(new GridLayout(0, 7));
        add(calendarPanel, BorderLayout.CENTER);

        // ===== Button actions =====
        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        searchBtn.addActionListener(e -> searchDialog());
        listViewBtn.addActionListener(e -> listViewDialog());
        statsBtn.addActionListener(e -> showStatistics());

        backupBtn.addActionListener(e -> backupDialog());
        restoreBtn.addActionListener(e -> restoreDialog());

        // Initial render
        updateCalendar();
        showUpcomingReminder();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ============================
    // Update calendar UI (month grid)
    // ============================
    private void updateCalendar() {
        calendarPanel.removeAll();
        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());

        // Days of week header
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            calendarPanel.add(lbl);
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7; // Sun=0, Mon=1...
        int daysInMonth = currentMonth.lengthOfMonth();

        // Empty labels for offset
        for (int i = 0; i < startDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Add day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            JButton dayBtn = new JButton(String.valueOf(day));
            LocalDate date = currentMonth.atDay(day);

            // Tooltip for events (uses BASIC SEARCH now)
            List<Event> dayEvents = manager.basicSearch(date);
            if (!dayEvents.isEmpty()) {
                StringBuilder tooltip = new StringBuilder();
                for (Event e : dayEvents) {
                    tooltip.append(e.getTitle())
                           .append(" (")
                           .append(String.format("%02d:%02d", e.getStart().getHour(), e.getStart().getMinute()))
                           .append(")")
                           .append(e.isRecurring() ? " [R]" : "")
                           .append("\n");
                }
                dayBtn.setToolTipText("<html>" + tooltip.toString().replace("\n", "<br>") + "</html>");
            }

            // Click day → Add or Manage
            dayBtn.addActionListener(ae -> {
                Object[] options = {"Add Event", "Manage Events"};
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "What do you want to do?",
                        "Day Options",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                if (choice == 0) addEventDialog(date);
                else if (choice == 1) manageEventsDialog(date);
            });

            calendarPanel.add(dayBtn);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    // ============================
    // FEATURE 3: Basic Search dialog
    // ============================
    private void searchDialog() {
        JTextField startDateField = new JTextField("yyyy-mm-dd");
        JTextField endDateField = new JTextField("yyyy-mm-dd (optional)");

        Object[] message = {
                "Start Date:", startDateField,
                "End Date (optional):", endDateField
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Basic Search (Date / Date Range)",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) return;

        try {
            LocalDate startDate = LocalDate.parse(startDateField.getText().trim());

            LocalDate endDate;
            String endText = endDateField.getText().trim();
            if (endText.isBlank() || endText.equalsIgnoreCase("yyyy-mm-dd (optional)")) {
                endDate = startDate; // single day search
            } else {
                endDate = LocalDate.parse(endText);
            }

            // IMPORTANT: hook to Feature 3
            List<Event> results = manager.basicSearch(startDate, endDate);

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No events found.");
                return;
            }

            StringBuilder sb = new StringBuilder("Events found:\n\n");
            for (Event e : results) {
                sb.append(e.getEventId())
                  .append(": ")
                  .append(e.getTitle())
                  .append(" | ")
                  .append(e.getStart())
                  .append(" -> ")
                  .append(e.getEnd())
                  .append(e.isRecurring() ? " (Recurring)" : "")
                  .append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Search Results", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use yyyy-mm-dd");
        }
    }

    // ============================
    // Add Event dialog (your existing feature set)
    // ============================
    private void addEventDialog(LocalDate date) {
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField startField = new JTextField("10:00");
        JTextField endField = new JTextField("11:00");

        JCheckBox recurringBox = new JCheckBox("Recurring Event");
        String[] types = {"DAILY", "WEEKLY", "MONTHLY"};
        JComboBox<String> recurrenceTypeBox = new JComboBox<>(types);
        JTextField repeatCountField = new JTextField("3");

        String[] reminderOptions = {"No reminder", "30 minutes before", "1 hour before", "1 day before"};
        JComboBox<String> reminderBox = new JComboBox<>(reminderOptions);

        Object[] message = {
                "Title:", titleField,
                "Description:", descField,
                "Start Time (HH:mm):", startField,
                "End Time (HH:mm):", endField,
                "Reminder:", reminderBox,
                recurringBox,
                "Recurrence Type:", recurrenceTypeBox,
                "Repeat Count:", repeatCountField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Event", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;

        try {
            LocalDateTime start = LocalDateTime.of(
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(startField.getText().split(":")[0]),
                    Integer.parseInt(startField.getText().split(":")[1])
            );

            LocalDateTime end = LocalDateTime.of(
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(endField.getText().split(":")[0]),
                    Integer.parseInt(endField.getText().split(":")[1])
            );

            if (manager.hasConflict(start, end)) {
                JOptionPane.showMessageDialog(this,
                        "This event conflicts with another event!",
                        "Conflict Detected",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Event event = new Event(manager.getNextEventId(), titleField.getText(), descField.getText(), start, end);

            int reminderMinutes = switch (reminderBox.getSelectedIndex()) {
                case 1 -> 30;
                case 2 -> 60;
                case 3 -> 1440;
                default -> 0;
            };
            event.setReminderMinutes(reminderMinutes);

            if (recurringBox.isSelected()) {
                event.setRecurring(true);
                event.setRecurrenceType((String) recurrenceTypeBox.getSelectedItem());
                event.setRecurrenceCount(Integer.parseInt(repeatCountField.getText()));
                manager.addRecurringEvent(event); // creates series
            } else {
                manager.createEvent(event.getTitle(), event.getDescription(), event.getStart(), event.getEnd());
            }

            updateCalendar();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
        }
    }

    // ============================
    // Manage events on a day (now uses basicSearch(date))
    // ============================
    private void manageEventsDialog(LocalDate date) {
        List<Event> dayEvents = manager.basicSearch(date);

        if (dayEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events on this day.");
            return;
        }

        String[] eventTitles = new String[dayEvents.size()];
        for (int i = 0; i < dayEvents.size(); i++) {
            Event e = dayEvents.get(i);
            eventTitles[i] = e.getEventId() + ": " + e.getTitle() +
                    " (" + String.format("%02d:%02d", e.getStart().getHour(), e.getStart().getMinute()) + ")" +
                    (e.isRecurring() ? " [Recurring]" : "");
        }

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select an event",
                "Manage Events",
                JOptionPane.PLAIN_MESSAGE,
                null,
                eventTitles,
                eventTitles[0]
        );

        if (selected == null) return;

        int selectedId = Integer.parseInt(selected.split(":")[0].trim());

        // IMPORTANT: find the real event by id from manager.getEvents() (not from dayEvents only)
        Event selectedEvent = null;
        for (Event e : manager.getEvents()) {
            if (e.getEventId() == selectedId) {
                selectedEvent = e;
                break;
            }
        }
        if (selectedEvent == null) return;

        Object[] options = {"Update", "Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Update or Delete?",
                "Event Options",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == JOptionPane.YES_OPTION) {
            updateEventDialog(selectedEvent);
        } else if (choice == JOptionPane.NO_OPTION) {
            if (selectedEvent.isRecurring()) {
                Object[] delOptions = {"This occurrence", "Entire series", "Cancel"};
                int delChoice = JOptionPane.showOptionDialog(
                        this,
                        "Delete this occurrence or entire series?",
                        "Delete Recurring Event",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        delOptions,
                        delOptions[0]
                );

                if (delChoice == 0) manager.deleteSingleOccurrence(selectedEvent);
                else if (delChoice == 1) manager.deleteRecurringEvent(selectedEvent);
            } else {
                manager.deleteEvent(selectedEvent.getEventId());
            }

            updateCalendar();
        }
    }

    // ============================
    // Update Event dialog
    // ============================
    private void updateEventDialog(Event event) {
        JTextField titleField = new JTextField(event.getTitle());
        JTextField descField = new JTextField(event.getDescription());
        JTextField startField = new JTextField(String.format("%02d:%02d", event.getStart().getHour(), event.getStart().getMinute()));
        JTextField endField = new JTextField(String.format("%02d:%02d", event.getEnd().getHour(), event.getEnd().getMinute()));

        Object[] message = {
                "Title:", titleField,
                "Description:", descField,
                "Start Time (HH:mm):", startField,
                "End Time (HH:mm):", endField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Update Event", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;

        try {
            LocalDate date = event.getStart().toLocalDate();

            LocalDateTime start = LocalDateTime.of(
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(startField.getText().split(":")[0]),
                    Integer.parseInt(startField.getText().split(":")[1])
            );

            LocalDateTime end = LocalDateTime.of(
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(endField.getText().split(":")[0]),
                    Integer.parseInt(endField.getText().split(":")[1])
            );

            if (manager.hasConflictExcludingEvent(start, end, event.getEventId())) {
                JOptionPane.showMessageDialog(this, "This event conflicts with another event!",
                        "Conflict Detected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (event.isRecurring()) {
                // Simple approach: update one occurrence only
                // If you want "update entire series", call manager.updateRecurringEvent(event) instead.
                manager.updateEvent(event.getEventId(), titleField.getText(), descField.getText(), start, end);
            } else {
                manager.updateEvent(event.getEventId(), titleField.getText(), descField.getText(), start, end);
            }

            updateCalendar();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
        }
    }

    // ============================
    // List View dialog (uses basicSearch ranges)
    // ============================
    private void listViewDialog() {
        String[] options = {"Day View", "Week View", "Month View"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose list view type",
                "List View",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == -1) return;

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();

        if (choice == 0) { // Day view
            sb.append("=== ").append(today).append(" ===\n");
            List<Event> dayEvents = manager.basicSearch(today);
            if (dayEvents.isEmpty()) sb.append("No events\n");
            else {
                for (Event e : dayEvents) {
                    sb.append(e.getTitle())
                      .append(" (")
                      .append(e.getStart().toLocalTime())
                      .append(")\n");
                }
            }
        }

        if (choice == 1) { // Week view
            LocalDate end = today.plusDays(6);
            sb.append("=== Week of ").append(today).append(" ===\n");
            for (LocalDate d = today; !d.isAfter(end); d = d.plusDays(1)) {
                sb.append(d.getDayOfWeek()).append(" ").append(d).append(":\n");
                List<Event> dayEvents = manager.basicSearch(d);
                if (dayEvents.isEmpty()) sb.append("  No events\n");
                else {
                    for (Event e : dayEvents) {
                        sb.append("  ").append(e.getTitle())
                          .append(" (").append(e.getStart().toLocalTime()).append(")\n");
                    }
                }
            }
        }

        if (choice == 2) { // Month view (current month)
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            sb.append("=== ").append(today.getMonth()).append(" ").append(today.getYear()).append(" ===\n");

            List<Event> monthEvents = manager.basicSearch(monthStart, monthEnd);
            if (monthEvents.isEmpty()) sb.append("No events\n");
            else {
                for (Event e : monthEvents) {
                    sb.append(e.getStart().toLocalDate())
                      .append(": ")
                      .append(e.getTitle())
                      .append(" (")
                      .append(e.getStart().toLocalTime())
                      .append(")\n");
                }
            }
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "List View", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================
    // Upcoming reminder popup (simple)
    // ============================
    private void showUpcomingReminder() {
        LocalDateTime now = LocalDateTime.now();
        Event nearest = null;

        for (Event e : manager.getEvents()) {
            if (e.getReminderMinutes() > 0 && e.getStart().isAfter(now)) {
                if (nearest == null || e.getStart().isBefore(nearest.getStart())) {
                    nearest = e;
                }
            }
        }

        if (nearest != null) {
            long minutes = Duration.between(now, nearest.getStart()).toMinutes();
            JOptionPane.showMessageDialog(
                    this,
                    "Upcoming Event:\n" +
                            nearest.getTitle() +
                            "\nStarts in " + minutes + " minutes"
            );
        }
    }

    private void showStatistics() {
        String message =
                "Event Statistics\n\n" +
                        "Total Events: " + manager.getTotalEvents() + "\n" +
                        "Recurring Events: " + manager.getRecurringEventCount() + "\n" +
                        "Busiest Day: " + manager.getBusiestDay();

        JOptionPane.showMessageDialog(this, message, "Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================
    // Backup / Restore dialogs
    // ============================
    private void backupDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Backup Location");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                manager.backupEvents(path);
                JOptionPane.showMessageDialog(this,
                        "Backup completed to:\n" + path,
                        "Backup Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error during backup:\n" + ex.getMessage(),
                        "Backup Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void restoreDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Backup File to Restore");
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                manager.restoreEvents(path);
                updateCalendar();
                JOptionPane.showMessageDialog(this,
                        "Restore completed from:\n" + path,
                        "Restore Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error during restore:\n" + ex.getMessage(),
                        "Restore Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarGUI(new EventManager()));
    }
}
