import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class TaskScheduler extends JFrame {

    private static final String DATA_FILE = "tasks.json";
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DefaultTableModel tableModel;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> futures =
            new ConcurrentHashMap<>();

    private JTable table;
    private JTextField nameField, timeField;

    public TaskScheduler() {
        super("Task Scheduler");
        setSize(800, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        applyDarkTheme();
        setupSystemTray();

        JPanel inputPanel = new JPanel();
        nameField = new JTextField(18);
        timeField = new JTextField(15);

        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        JButton saveBtn = new JButton("Save JSON");

        inputPanel.add(new JLabel("Task"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Time"));
        inputPanel.add(timeField);
        inputPanel.add(addBtn);
        inputPanel.add(editBtn);
        inputPanel.add(delBtn);
        inputPanel.add(saveBtn);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Task", "Time", "Status"}, 0);
        table = new JTable(tableModel);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        addBtn.addActionListener(e -> addTask());
        editBtn.addActionListener(e -> editTask());
        delBtn.addActionListener(e -> deleteTask());
        saveBtn.addActionListener(e -> saveTasks());

        loadTasks();
    }

    private void addTask() {
        try {
            String name = nameField.getText();
            LocalDateTime time =
                LocalDateTime.parse(timeField.getText(), FORMAT);

            String id = UUID.randomUUID().toString();
            tableModel.addRow(new Object[]{
                    id, name, time.format(FORMAT), "Scheduled"
            });

            scheduleTask(id, name, time);
            nameField.setText("");
            timeField.setText("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format");
        }
    }

    private void editTask() {
    int row = table.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "Select a task to edit");
        return;
    }

    String id = tableModel.getValueAt(row, 0).toString();
    String newName = JOptionPane.showInputDialog("New Task Name:");
    String newTime = JOptionPane.showInputDialog(
            "New Time (yyyy-MM-dd HH:mm):");

    try {
        LocalDateTime time =
                LocalDateTime.parse(newTime, FORMAT);

        tableModel.setValueAt(newName, row, 1);
        tableModel.setValueAt(newTime, row, 2);

        // SAFELY cancel previous schedule
        if (futures.containsKey(id)) {
            futures.get(id).cancel(false);
            futures.remove(id);
        }

        scheduleTask(id, newName, time);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Invalid Update");
    }
}


    private void deleteTask() {
    int row = table.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "Select a task to delete");
        return;
    }

    String id = tableModel.getValueAt(row, 0).toString();

    // SAFELY cancel only if present
    if (futures.containsKey(id)) {
        futures.get(id).cancel(false);
        futures.remove(id);
    }

    tableModel.removeRow(row);
}

    private void scheduleTask(String id, String name,
                              LocalDateTime time) {

        long delay = Duration.between(
                LocalDateTime.now(), time).toMillis();

        if (delay < 0) return;

        ScheduledFuture<?> f =
                scheduler.schedule(() -> {
                    showNotification(name);
                    updateStatus(id, "Done");
                }, delay, TimeUnit.MILLISECONDS);

        futures.put(id, f);
    }

    private void updateStatus(String id, String status) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0)
                        .equals(id)) {
                    tableModel.setValueAt(status, i, 3);
                }
            }
        });
    }


    private void saveTasks() {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {

        bw.write("[\n");

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            bw.write("  {\"id\":\"" + tableModel.getValueAt(i, 0) + "\",");
            bw.write("\"task\":\"" + tableModel.getValueAt(i, 1) + "\",");
            bw.write("\"time\":\"" + tableModel.getValueAt(i, 2) + "\",");
            bw.write("\"status\":\"" + tableModel.getValueAt(i, 3) + "\"}");

            if (i < tableModel.getRowCount() - 1)
                bw.write(",");

            bw.newLine();
        }

        bw.write("]");

        JOptionPane.showMessageDialog(this, "All Tasks Saved Successfully!");

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Save Failed!");
    }
}

private void loadTasks() {
    if (!Files.exists(Paths.get(DATA_FILE))) return;

    try {
        String json = Files.readString(Paths.get(DATA_FILE))
                .replace("[", "")
                .replace("]", "")
                .trim();

        if (json.isEmpty()) return;

        String[] objects = json.split("\\},\\s*\\{");

        for (String obj : objects) {

            obj = obj.replace("{", "").replace("}", "");

            String[] pairs = obj.split(",");
            Map<String, String> map = new HashMap<>();

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                map.put(
                        keyValue[0].replace("\"", ""),
                        keyValue[1].replace("\"", "")
                );
            }

            String id = map.get("id");
            String task = map.get("task");
            String time = map.get("time");
            String status = map.get("status");

            tableModel.addRow(new Object[]{id, task, time, status});

            if (status.equals("Scheduled")) {
                scheduleTask(id, task,
                        LocalDateTime.parse(time, FORMAT));
            }
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Load Failed!");
    }
}

    


    private void applyDarkTheme() {
        UIManager.put("panel.background", Color.DARK_GRAY);
        UIManager.put("Table.background", Color.BLACK);
        UIManager.put("Table.foreground", Color.GREEN);
        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Button.background", Color.GRAY);
        UIManager.put("Button.foreground", Color.RED);
    }


    private void setupSystemTray() {

        if (!SystemTray.isSupported()) return;

        try {
            TrayIcon tray = new TrayIcon(
                Toolkit.getDefaultToolkit()
                        .createImage(""),
                "Task Scheduler");

            tray.setImageAutoSize(true);
            SystemTray.getSystemTray().add(tray);
        } catch (Exception e) {}
    }

    private void showNotification(String msg) {

        if (!SystemTray.isSupported()) return;

        for (TrayIcon icon :
                SystemTray.getSystemTray().getTrayIcons()) {

            icon.displayMessage("Task Alert",
                    msg, TrayIcon.MessageType.INFO);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater
            (() -> new TaskScheduler().setVisible(true));
    }
}
