package calendarapp;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public class CalendarGUI extends JFrame {

    private JLabel monthLabel;
    private JPanel calendarPanel;
    private YearMonth currentMonth;
    private EventManager manager; // link to EventManager


    // Constructor
    public CalendarGUI(EventManager manager) {
        this.manager = manager; // connect EventManager
        currentMonth = YearMonth.now();

        setTitle("Calendar App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel with month label and navigation buttons
        JPanel topPanel = new JPanel();
        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);

        topPanel.add(prevBtn);
        topPanel.add(monthLabel);
        topPanel.add(nextBtn);

        JButton searchBtn = new JButton("Search");
topPanel.add(searchBtn);
JButton listViewBtn = new JButton("List View");
topPanel.add(listViewBtn);

listViewBtn.addActionListener(e -> listViewDialog());

JButton statsBtn = new JButton("Statistics");
topPanel.add(statsBtn);

statsBtn.addActionListener(e -> showStatistics());

searchBtn.addActionListener(e -> searchDialog());
        add(topPanel, BorderLayout.NORTH);

        // Calendar panel for days
        calendarPanel = new JPanel(new GridLayout(0, 7));
        add(calendarPanel, BorderLayout.CENTER);

        // Navigation button actions
        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        // Initial calendar render
        updateCalendar();
        showUpcomingReminder();
        setVisible(true);
        
        // Backup and Restore Buttons
JButton backupBtn = new JButton("Backup");
topPanel.add(backupBtn);

backupBtn.addActionListener(e -> {
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
});

JButton restoreBtn = new JButton("Restore");
topPanel.add(restoreBtn);

restoreBtn.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select Backup File to Restore");
    int userSelection = fileChooser.showOpenDialog(this);

    if (userSelection == JFileChooser.APPROVE_OPTION) {
        String path = fileChooser.getSelectedFile().getAbsolutePath();
        try {
            manager.restoreEvents(path);
            updateCalendar(); // Refresh calendar after restore
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
});

    }

    // Update calendar UI
    private void updateCalendar() {
        calendarPanel.removeAll();
        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());

        // Days of week
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            calendarPanel.add(new JLabel(day, SwingConstants.CENTER));
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();

        // Empty labels for offset
        for (int i = 0; i < startDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Add day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            JButton dayBtn = new JButton(String.valueOf(day));
            LocalDate date = currentMonth.atDay(day);

            // Tooltip for events
            StringBuilder tooltip = new StringBuilder();
            for (Event e : manager.getEvents()) {
                if (e.getStart().toLocalDate().equals(date)) {
                    tooltip.append(e.getTitle())
                           .append(" (")
                           .append(e.getStart().getHour())
                           .append(":")
                           .append(e.getStart().getMinute())
                           .append(")\n");
                }
            }
            if (tooltip.length() > 0) {
                dayBtn.setToolTipText("<html>" + tooltip.toString().replaceAll("\n", "<br>") + "</html>");
            }

            // Click to choose Add or Manage Events
            dayBtn.addActionListener(ae -> {
                Object[] options = {"Add Event", "Manage Events"};
                int choice = JOptionPane.showOptionDialog(this, "What do you want to do?",
                        "Day Options", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);

                if (choice == 0) {
                    addEventDialog(date); // Add new event
                } else if (choice == 1) {
                    manageEventsDialog(date); // Update/Delete existing events
                }
            });

            calendarPanel.add(dayBtn);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    
    } 
    
    private void searchDialog() {
    // Basic search fields
    JTextField startDateField = new JTextField("yyyy-mm-dd");
    JTextField endDateField = new JTextField("yyyy-mm-dd (optional)");

    // Advanced search fields
    JTextField titleField = new JTextField("Leave blank if not filtering");
    JCheckBox recurringBox = new JCheckBox("Recurring only");

    Object[] message = {
            "Start Date:", startDateField,
            "End Date (optional):", endDateField,
            "Title contains (optional):", titleField,
            recurringBox
    };

    int option = JOptionPane.showConfirmDialog(
            this,
            message,
            "Search Events",
            JOptionPane.OK_CANCEL_OPTION
    );

    if (option == JOptionPane.OK_OPTION) {
        try {
            LocalDate startDate = LocalDate.parse(startDateField.getText());
            LocalDate endDate;
            if (endDateField.getText().isBlank() || endDateField.getText().equals("yyyy-mm-dd (optional)")) {
                endDate = startDate; // single date search
            } else {
                endDate = LocalDate.parse(endDateField.getText());
            }

            java.util.List<Event> results = manager.searchByDateRange(startDate, endDate);

            // Filter by title keyword
            String keyword = titleField.getText().trim().toLowerCase();
            if (!keyword.isBlank()) {
                results.removeIf(e -> !e.getTitle().toLowerCase().contains(keyword));
            }

            // Filter by recurring
            if (recurringBox.isSelected()) {
                results.removeIf(e -> !e.isRecurring());
            }

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No events found.");
                return;
            }

            // Display results
            StringBuilder sb = new StringBuilder("Events found:\n\n");
            for (Event e : results) {
                sb.append(e.getEventId())
                  .append(": ")
                  .append(e.getTitle())
                  .append(" | ")
                  .append(e.getStart())
                  .append(e.isRecurring() ? " (Recurring)" : "")
                  .append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Search Results",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format.");
        }
    }
}




    // Popup to update an existing event
    private void updateEventDialog(Event event) {
    JTextField titleField = new JTextField(event.getTitle());
    JTextField descField = new JTextField(event.getDescription());
    JTextField startField = new JTextField(event.getStart().getHour() + ":" + event.getStart().getMinute());
    JTextField endField = new JTextField(event.getEnd().getHour() + ":" + event.getEnd().getMinute());

    JCheckBox recurringBox = new JCheckBox("Recurring Event", event.isRecurring());
    String[] types = {"DAILY", "WEEKLY", "MONTHLY"};
    JComboBox<String> recurrenceTypeBox = new JComboBox<>(types);
    recurrenceTypeBox.setSelectedItem(event.getRecurrenceType());
    JTextField repeatCountField = new JTextField(String.valueOf(event.getRecurrenceCount()));

    Object[] message = {
            "Title:", titleField,
            "Description:", descField,
            "Start Time (HH:mm):", startField,
            "End Time (HH:mm):", endField,
            recurringBox,
            "Recurrence Type:", recurrenceTypeBox,
            "Repeat Count:", repeatCountField
    };

    int option = JOptionPane.showConfirmDialog(this, message, "Update Event", JOptionPane.OK_CANCEL_OPTION);
    if (option == JOptionPane.OK_OPTION) {
        try {
            LocalDate date = event.getStart().toLocalDate();
            LocalDateTime start = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(startField.getText().split(":")[0]),
                    Integer.parseInt(startField.getText().split(":")[1]));
            LocalDateTime end = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    Integer.parseInt(endField.getText().split(":")[0]),
                    Integer.parseInt(endField.getText().split(":")[1]));

            if (manager.hasConflictExcludingEvent(start, end, event.getEventId())) {
                JOptionPane.showMessageDialog(this, "This event conflicts with another event!", "Conflict Detected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            event.setTitle(titleField.getText());
            event.setDescription(descField.getText());
            event.setStart(start);
            event.setEnd(end);

            if (recurringBox.isSelected()) {
                event.setRecurring(true);
                event.setRecurrenceType((String) recurrenceTypeBox.getSelectedItem());
                event.setRecurrenceCount(Integer.parseInt(repeatCountField.getText()));
                manager.updateRecurringEvent(event);
            } else {
                event.setRecurring(false);
                manager.updateEvent(event.getEventId(), event.getTitle(), event.getDescription(), start, end);
            }

            updateCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
        }
    }
}


    // Popup to add a new event
private void addEventDialog(LocalDate date) {
    JTextField titleField = new JTextField();
    JTextField descField = new JTextField();
    JTextField startField = new JTextField("10:00");
    JTextField endField = new JTextField("11:00");

    JCheckBox recurringBox = new JCheckBox("Recurring Event");
    String[] reminderOptions = {"No reminder", "30 minutes before", "1 hour before", "1 day before"};
    JComboBox<String> reminderBox = new JComboBox<>(reminderOptions);

    String[] types = {"DAILY", "WEEKLY", "MONTHLY"};
    JComboBox<String> recurrenceTypeBox = new JComboBox<>(types);
    JTextField repeatCountField = new JTextField("3");

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

    if (option == JOptionPane.OK_OPTION) {
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

            // Conflict check before creating event
            if (manager.hasConflict(start, end)) {
                JOptionPane.showMessageDialog(this,
                        "This event conflicts with another event!",
                        "Conflict Detected",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            Event event = new Event(manager.getNextEventId(), titleField.getText(), descField.getText(), start, end);

            // Reminder
            int reminderMinutes = switch (reminderBox.getSelectedIndex()) {
                case 1 -> 30;
                case 2 -> 60;
                case 3 -> 1440;
                default -> 0;
            };
            event.setReminderMinutes(reminderMinutes);

            // Recurring setup
            if (recurringBox.isSelected()) {
                event.setRecurring(true);
                event.setRecurrenceType((String) recurrenceTypeBox.getSelectedItem());
                event.setRecurrenceCount(Integer.parseInt(repeatCountField.getText()));
                manager.addRecurringEvent(event); // manager handles series creation
            } else {
                manager.createEvent(event.getTitle(), event.getDescription(), event.getStart(), event.getEnd());
            }

            updateCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
        }
    }
}



    // Popup to manage existing events on a day
    private void manageEventsDialog(LocalDate date) {
    java.util.List<Event> dayEvents = new java.util.ArrayList<>();
    for (Event e : manager.getEvents()) {
        if (e.getStart().toLocalDate().equals(date)) dayEvents.add(e);
    }

    if (dayEvents.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No events on this day.");
        return;
    }

    String[] eventTitles = new String[dayEvents.size()];
    for (int i = 0; i < dayEvents.size(); i++) {
        Event e = dayEvents.get(i);
        eventTitles[i] = e.getEventId() + ": " + e.getTitle() + " (" + e.getStart().getHour() + ":" + e.getStart().getMinute() + ")";
    }

    String selected = (String) JOptionPane.showInputDialog(this, "Select an event", "Manage Events",
            JOptionPane.PLAIN_MESSAGE, null, eventTitles, eventTitles[0]);
    if (selected == null) return;

    int selectedId = Integer.parseInt(selected.split(":")[0]);
    Event selectedEvent = dayEvents.stream().filter(e -> e.getEventId() == selectedId).findFirst().orElse(null);
    if (selectedEvent == null) return;

    Object[] options = {"Update", "Delete", "Cancel"};
    int choice = JOptionPane.showOptionDialog(this, "Update or Delete?", "Event Options",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    if (choice == JOptionPane.YES_OPTION) updateEventDialog(selectedEvent);
    else if (choice == JOptionPane.NO_OPTION) {
        if (selectedEvent.isRecurring()) {
            Object[] delOptions = {"This occurrence", "Entire series", "Cancel"};
            int delChoice = JOptionPane.showOptionDialog(this, "Delete this occurrence or entire series?",
                    "Delete Recurring Event", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, delOptions, delOptions[0]);

            if (delChoice == 0) manager.deleteSingleOccurrence(selectedEvent);
            else if (delChoice == 1) manager.deleteRecurringEvent(selectedEvent);
        } else manager.deleteEvent(selectedEvent.getEventId());

        updateCalendar();
    }
}


    // Main method to launch GUI
    public static void main(String[] args) {
    EventManager manager = new EventManager();

    // TEMP TEST FOR FEATURE 4
    manager.createEvent(
        "Test Event",
        "Backup check",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
    );

    manager.backupEvents("backup_events.csv");

    manager.restoreEvents("backup_events.csv");

    new CalendarGUI(manager);
}
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
        long minutes = java.time.Duration.between(now, nearest.getStart()).toMinutes();
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

    JOptionPane.showMessageDialog(
            this,
            message,
            "Statistics",
            JOptionPane.INFORMATION_MESSAGE
    );
}
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

    if (choice == 0) { // DAY VIEW
        sb.append("=== ").append(today).append(" ===\n");
        boolean found = false;
        for (Event e : manager.getEvents()) {
            if (e.getStart().toLocalDate().equals(today)) {
                sb.append(e.getTitle())
                  .append(" (")
                  .append(e.getStart().toLocalTime())
                  .append(")\n");
                found = true;
            }
        }
        if (!found) sb.append("No events\n");
    }

    if (choice == 1) { // WEEK VIEW
        sb.append("=== Week of ").append(today).append(" ===\n");
        LocalDate end = today.plusDays(6);
        for (LocalDate d = today; !d.isAfter(end); d = d.plusDays(1)) {
            sb.append(d.getDayOfWeek()).append(" ").append(d).append(":\n");
            boolean found = false;
            for (Event e : manager.getEvents()) {
                if (e.getStart().toLocalDate().equals(d)) {
                    sb.append("  ")
                      .append(e.getTitle())
                      .append(" (")
                      .append(e.getStart().toLocalTime())
                      .append(")\n");
                    found = true;
                }
            }
            if (!found) sb.append("  No events\n");
        }
    }

    if (choice == 2) { // MONTH VIEW
        sb.append("=== ").append(today.getMonth())
          .append(" ").append(today.getYear()).append(" ===\n");
        for (Event e : manager.getEvents()) {
            if (e.getStart().getMonth() == today.getMonth()) {
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


    }

