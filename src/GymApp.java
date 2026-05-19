import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GymApp extends JFrame {
    private final Map<DayOfWeek, ArrayList<ExerciseData>> weeklyRoutine = new HashMap<>();
    private final ArrayList<LocalDate> timelineDates = new ArrayList<>();
    private final Map<String, Screen> screens = new HashMap<>();
    private final ArrayList<BodyWeightEntry> bodyEntries = new ArrayList<>();

    private LocalDate currentSelectedDate;
    private CardLayout cardLayout;
    private JPanel containerPanel;
    private JPanel exerciseListPanel;
    private JTextArea assessorFeedback;
    private boolean isEditMode;
    private JButton globalEditBtn;
    private RoundedPanel selectedDayBox;
    private Point dragStartPoint;

    private final String[] macroLabels = {"Calories", "Protein", "Carbs", "Fats"};
    private final String[] macroValues = {"2700 kcal", "165g", "310g", "75g"};
    private boolean isMacroEditMode;
    private JPanel macroCardsContainer;
    private JButton macroEditBtn;

    private final String[] prLabels = {"Bench Press", "Squat", "Deadlift"};
    private final String[] prValues = {"100 kg", "140 kg", "180 kg"};
    private boolean isPrEditMode;
    private JPanel prCardsContainer;
    private JButton prEditBtn;

    private JComboBox<String> historyExerciseSelect;
    private BarChartPanel historyChart;
    private JPanel bodyLogPanel;
    private LineChartPanel bodyChart;
    private double heightCm = 178;

    public GymApp() {
        setTitle("IronPulse Gym Tracker");
        setSize(480, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        setContentPane(containerPanel);

        for (DayOfWeek day : DayOfWeek.values()) {
            weeklyRoutine.put(day, new ArrayList<>());
        }
        seedData();

        addScreen("WORKOUT", createMainWorkoutScreen());
        addScreen("MACRO", createMacroTrackerScreen());
        addScreen("PR", createPersonalRecordsScreen());
        addScreen("HISTORY", createHistoryScreen());
        addScreen("BODY", createBodyScreen());
        navigateTo("WORKOUT");
    }

    private void seedData() {
        LocalDate today = LocalDate.now();
        currentSelectedDate = today;
        for (int i = -13; i <= 14; i++) {
            timelineDates.add(today.plusDays(i));
        }
        weeklyRoutine.get(today.getDayOfWeek()).add(new ExerciseData("Bench Press", "82.5", "4x8"));
        weeklyRoutine.get(today.minusDays(2).getDayOfWeek()).add(new ExerciseData("Squat", "105", "5x5"));
        weeklyRoutine.get(today.minusDays(4).getDayOfWeek()).add(new ExerciseData("Deadlift", "130", "3x5"));
        bodyEntries.add(new BodyWeightEntry(today.minusDays(12), 82.6));
        bodyEntries.add(new BodyWeightEntry(today.minusDays(9), 82.1));
        bodyEntries.add(new BodyWeightEntry(today.minusDays(6), 81.8));
        bodyEntries.add(new BodyWeightEntry(today.minusDays(3), 81.5));
        bodyEntries.add(new BodyWeightEntry(today, 81.2));
    }

    private void addScreen(String name, JPanel screen) {
        containerPanel.add(screen, name);
        if (screen instanceof Screen) {
            screens.put(name, (Screen) screen);
        }
    }

    private void navigateTo(String name) {
        cardLayout.show(containerPanel, name);
        Screen screen = screens.get(name);
        if (screen != null) {
            screen.onNavigateTo();
        }
    }

    private AppScreen createScreenShell(String title, String iconType) {
        AppScreen screen = new AppScreen(new BorderLayout());
        screen.setBackground(Theme.BG_DARK);
        screen.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        screen.add(new IconHeader(title, iconType), BorderLayout.NORTH);
        screen.add(createBottomNav(title), BorderLayout.SOUTH);
        return screen;
    }

    private JPanel createBottomNav(String activeTitle) {
        JPanel nav = new RoundedPanel(22, Theme.PANEL_DARK);
        nav.setLayout(new GridLayout(1, 5, 4, 0));
        nav.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        nav.setPreferredSize(new Dimension(430, 68));
        nav.add(navButton("WORKOUT", "Lift", activeTitle.equals("Workout"), "workout"));
        nav.add(navButton("MACRO", "Macros", activeTitle.equals("Macros"), "macro"));
        nav.add(navButton("PR", "PRs", activeTitle.equals("Records"), "pr"));
        nav.add(navButton("HISTORY", "History", activeTitle.equals("History"), "history"));
        nav.add(navButton("BODY", "Body", activeTitle.equals("Body"), "body"));
        return nav;
    }

    private JButton navButton(String target, String label, boolean active, String iconType) {
        JButton button = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(Theme.CARD_BG_SELECTED);
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                    g2.setColor(Theme.ACCENT_GLOW);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 16, 16);
                }
                g2.setColor(active ? Theme.ACCENT : Theme.TEXT_MUTED);
                drawMiniIcon(g2, iconType, getWidth() / 2 - 10, 9);
                g2.setFont(Theme.SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, 48);
                g2.dispose();
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.addActionListener(e -> navigateTo(target));
        return button;
    }

    private void drawMiniIcon(Graphics2D g2, String type, int x, int y) {
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if ("workout".equals(type)) {
            g2.drawLine(x, y + 10, x + 20, y + 10);
            g2.drawLine(x + 3, y + 5, x + 3, y + 15);
            g2.drawLine(x + 17, y + 5, x + 17, y + 15);
        } else if ("macro".equals(type)) {
            g2.drawOval(x + 2, y + 1, 16, 16);
            g2.drawLine(x + 10, y + 9, x + 17, y + 3);
        } else if ("pr".equals(type)) {
            g2.drawLine(x + 10, y + 2, x + 10, y + 17);
            g2.drawLine(x + 4, y + 8, x + 10, y + 2);
            g2.drawLine(x + 16, y + 8, x + 10, y + 2);
        } else if ("history".equals(type)) {
            g2.fillRect(x + 2, y + 11, 4, 7);
            g2.fillRect(x + 8, y + 7, 4, 11);
            g2.fillRect(x + 14, y + 3, 4, 15);
        } else {
            g2.drawLine(x + 1, y + 18, x + 19, y + 18);
            g2.drawLine(x + 1, y + 18, x + 1, y + 2);
            g2.drawLine(x + 1, y + 14, x + 8, y + 10);
            g2.drawLine(x + 8, y + 10, x + 13, y + 12);
            g2.drawLine(x + 13, y + 12, x + 19, y + 4);
        }
    }

    private JPanel createMainWorkoutScreen() {
        AppScreen mainWrapper = createScreenShell("Workout", "workout");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_DARK);

        content.add(createTimelinePanel());
        content.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel targetHeaderPanel = new JPanel(new BorderLayout());
        targetHeaderPanel.setOpaque(false);
        targetHeaderPanel.setMaximumSize(new Dimension(440, 35));
        JLabel logHeader = new JLabel("Today's Targets:");
        logHeader.setForeground(Theme.TEXT);
        logHeader.setFont(Theme.HEADER_FONT);
        targetHeaderPanel.add(logHeader, BorderLayout.WEST);
        globalEditBtn = new RoundedButton("Edit Logs", 10);
        globalEditBtn.setBackground(Theme.PANEL_MID);
        globalEditBtn.setForeground(Theme.TEXT_MUTED);
        globalEditBtn.setPreferredSize(new Dimension(100, 30));
        targetHeaderPanel.add(globalEditBtn, BorderLayout.EAST);
        content.add(targetHeaderPanel);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        exerciseListPanel = new JPanel();
        exerciseListPanel.setLayout(new BoxLayout(exerciseListPanel, BoxLayout.Y_AXIS));
        exerciseListPanel.setBackground(Theme.BG_DARK);
        JScrollPane listScroll = new JScrollPane(exerciseListPanel);
        listScroll.setPreferredSize(new Dimension(420, 220));
        styleScroll(listScroll);
        content.add(listScroll);

        content.add(Box.createRigidArea(new Dimension(0, 12)));
        JButton assessBtn = new RoundedButton("Run Weekly Split Assessment", 15);
        assessBtn.setBackground(Theme.ACCENT);
        assessBtn.setForeground(Color.WHITE);
        assessBtn.setMaximumSize(new Dimension(420, 42));
        content.add(assessBtn);

        content.add(Box.createRigidArea(new Dimension(0, 10)));
        assessorFeedback = new JTextArea("Assessment Report will appear here...");
        assessorFeedback.setEditable(false);
        assessorFeedback.setLineWrap(true);
        assessorFeedback.setWrapStyleWord(true);
        assessorFeedback.setBackground(Theme.BG_DARK);
        assessorFeedback.setForeground(Theme.TEXT_MUTED);
        assessorFeedback.setFont(Theme.BODY);
        content.add(assessorFeedback);

        JButton fabAddBtn = new RoundedButton("+ Add Exercise", 15);
        fabAddBtn.setBackground(Theme.ACCENT);
        fabAddBtn.setForeground(Color.WHITE);
        fabAddBtn.setMaximumSize(new Dimension(420, 42));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(fabAddBtn);

        mainWrapper.add(content, BorderLayout.CENTER);
        mainWrapper.setNavigateHook(this::refreshExerciseListUI);
        wireWorkoutActions(assessBtn, fabAddBtn);
        return mainWrapper;
    }

    private JScrollPane createTimelinePanel() {
        JPanel calendarPanel = new JPanel();
        calendarPanel.setLayout(new BoxLayout(calendarPanel, BoxLayout.X_AXIS));
        calendarPanel.setBackground(Theme.BG_DARK);
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE");
        DateTimeFormatter numFormatter = DateTimeFormatter.ofPattern("dd");
        for (LocalDate date : timelineDates) {
            RoundedPanel dayBox = new RoundedPanel(15, date.equals(currentSelectedDate) ? Theme.CARD_BG_SELECTED : Theme.PANEL_DARK);
            dayBox.setSelectedGlow(date.equals(currentSelectedDate));
            dayBox.setLayout(new GridLayout(2, 1));
            dayBox.setPreferredSize(new Dimension(65, 65));
            dayBox.setMaximumSize(new Dimension(65, 65));
            if (date.equals(currentSelectedDate)) {
                selectedDayBox = dayBox;
            }
            JLabel dayLabel = new JLabel(date.format(dayFormatter), SwingConstants.CENTER);
            JLabel numLabel = new JLabel(date.format(numFormatter), SwingConstants.CENTER);
            dayLabel.setForeground(Theme.TEXT_MUTED);
            numLabel.setForeground(Theme.TEXT);
            numLabel.setFont(Theme.HEADER_FONT);
            dayBox.add(dayLabel);
            dayBox.add(numLabel);
            dayBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (selectedDayBox != null) {
                        selectedDayBox.setBackgroundColor(Theme.PANEL_DARK);
                        selectedDayBox.setSelectedGlow(false);
                    }
                    selectedDayBox = dayBox;
                    currentSelectedDate = date;
                    dayBox.setBackgroundColor(Theme.CARD_BG_SELECTED);
                    dayBox.setSelectedGlow(true);
                    refreshExerciseListUI();
                }
            });
            calendarPanel.add(dayBox);
            calendarPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        }
        JScrollPane calendarScroll = new JScrollPane(calendarPanel);
        calendarScroll.setPreferredSize(new Dimension(420, 80));
        calendarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        calendarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        styleScroll(calendarScroll);
        MouseAdapter dragScrollListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    JViewport viewport = calendarScroll.getViewport();
                    Point position = viewport.getViewPosition();
                    position.x += dragStartPoint.x - e.getX();
                    position.x = Math.max(0, Math.min(position.x, calendarPanel.getWidth() - viewport.getWidth()));
                    viewport.setViewPosition(position);
                }
            }
        };
        calendarPanel.addMouseListener(dragScrollListener);
        calendarPanel.addMouseMotionListener(dragScrollListener);
        return calendarScroll;
    }

    private void wireWorkoutActions(JButton assessBtn, JButton fabAddBtn) {
        globalEditBtn.addActionListener(e -> {
            isEditMode = !isEditMode;
            globalEditBtn.setText(isEditMode ? "Done" : "Edit Logs");
            globalEditBtn.setBackground(isEditMode ? Theme.DANGER : Theme.PANEL_MID);
            globalEditBtn.setForeground(isEditMode ? Color.WHITE : Theme.TEXT_MUTED);
            refreshExerciseListUI();
        });
        fabAddBtn.addActionListener(e -> addExerciseDialog());
        assessBtn.addActionListener(e -> runAssessment());
    }

    private JPanel createMacroTrackerScreen() {
        AppScreen panel = createEditableStatsScreen("Macros", "macro", macroLabels, macroValues, true);
        panel.setNavigateHook(() -> refreshStatCards(macroCardsContainer, macroLabels, macroValues, isMacroEditMode));
        return panel;
    }

    private JPanel createPersonalRecordsScreen() {
        AppScreen panel = createEditableStatsScreen("Records", "pr", prLabels, prValues, false);
        panel.setNavigateHook(() -> refreshStatCards(prCardsContainer, prLabels, prValues, isPrEditMode));
        return panel;
    }

    private AppScreen createEditableStatsScreen(String title, String icon, String[] labels, String[] values, boolean macro) {
        AppScreen panel = createScreenShell(title, icon);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JButton editBtn = new RoundedButton("Edit Stats", 10);
        editBtn.setPreferredSize(new Dimension(110, 32));
        JPanel tools = new JPanel(new BorderLayout());
        tools.setOpaque(false);
        tools.add(editBtn, BorderLayout.EAST);
        center.add(tools, BorderLayout.NORTH);
        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBackground(Theme.BG_DARK);
        JScrollPane scroll = new JScrollPane(cards);
        styleScroll(scroll);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        if (macro) {
            macroCardsContainer = cards;
            macroEditBtn = editBtn;
        } else {
            prCardsContainer = cards;
            prEditBtn = editBtn;
        }
        editBtn.addActionListener(e -> {
            if (macro) {
                isMacroEditMode = !isMacroEditMode;
                updateEditButton(macroEditBtn, isMacroEditMode);
                refreshStatCards(macroCardsContainer, labels, values, isMacroEditMode);
            } else {
                isPrEditMode = !isPrEditMode;
                updateEditButton(prEditBtn, isPrEditMode);
                refreshStatCards(prCardsContainer, labels, values, isPrEditMode);
            }
        });
        refreshStatCards(cards, labels, values, false);
        return panel;
    }

    private void updateEditButton(JButton button, boolean editing) {
        button.setText(editing ? "Save" : "Edit Stats");
        button.setBackground(editing ? Theme.ACCENT : Theme.PANEL_MID);
        button.setForeground(editing ? Color.WHITE : Theme.TEXT_MUTED);
    }

    private void refreshStatCards(JPanel container, String[] labels, String[] values, boolean editMode) {
        container.removeAll();
        container.add(Box.createRigidArea(new Dimension(0, 20)));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            container.add(new StatCardPanel(labels[index], values[index], editMode, value -> values[index] = value));
            container.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        container.revalidate();
        container.repaint();
    }

    private JPanel createHistoryScreen() {
        AppScreen screen = createScreenShell("History", "history");
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        historyExerciseSelect = new JComboBox<>();
        historyExerciseSelect.setBackground(Theme.FIELD_BG);
        historyExerciseSelect.setForeground(Theme.TEXT);
        historyExerciseSelect.addActionListener(e -> updateHistoryChart());
        center.add(historyExerciseSelect, BorderLayout.NORTH);
        historyChart = new BarChartPanel();
        center.add(historyChart, BorderLayout.CENTER);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(() -> {
            refreshExerciseChoices();
            updateHistoryChart();
        });
        return screen;
    }

    private JPanel createBodyScreen() {
        AppScreen screen = createScreenShell("Body", "body");
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        JPanel controls = new JPanel(new GridLayout(1, 2, 8, 0));
        controls.setOpaque(false);
        JButton heightBtn = new RoundedButton("Height: " + (int) heightCm + " cm", 12);
        JButton addWeightBtn = new RoundedButton("+ Weight", 12);
        heightBtn.setBackground(Theme.PANEL_MID);
        addWeightBtn.setBackground(Theme.ACCENT);
        controls.add(heightBtn);
        controls.add(addWeightBtn);
        center.add(controls, BorderLayout.NORTH);
        bodyChart = new LineChartPanel();
        center.add(bodyChart, BorderLayout.CENTER);
        bodyLogPanel = new JPanel();
        bodyLogPanel.setLayout(new BoxLayout(bodyLogPanel, BoxLayout.Y_AXIS));
        bodyLogPanel.setBackground(Theme.BG_DARK);
        JScrollPane logScroll = new JScrollPane(bodyLogPanel);
        logScroll.setPreferredSize(new Dimension(420, 160));
        styleScroll(logScroll);
        center.add(logScroll, BorderLayout.SOUTH);
        screen.add(center, BorderLayout.CENTER);
        heightBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Height in cm:", heightCm);
            if (input != null) {
                try {
                    heightCm = Double.parseDouble(input.trim());
                    heightBtn.setText("Height: " + (int) heightCm + " cm");
                    refreshBodyUI();
                } catch (NumberFormatException ignored) {
                    JOptionPane.showMessageDialog(this, "Enter a valid height.");
                }
            }
        });
        addWeightBtn.addActionListener(e -> addBodyEntryDialog());
        screen.setNavigateHook(this::refreshBodyUI);
        return screen;
    }

    private void addExerciseDialog() {
        JTextField nameField = new JTextField();
        JTextField weightField = new JTextField();
        JTextField repField = new JTextField();
        Object[] message = {"Exercise Name:", nameField, "Weight (kg/lbs):", weightField, "Rep Range (e.g. 4x10):", repField};
        int option = JOptionPane.showConfirmDialog(this, message, "Log New Exercise", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            weeklyRoutine.get(currentSelectedDate.getDayOfWeek()).add(new ExerciseData(nameField.getText().trim(), weightField.getText().trim(), repField.getText().trim()));
            refreshExerciseListUI();
        }
    }

    private void runAssessment() {
        ArrayList<ExerciseData> currentLogs = weeklyRoutine.get(currentSelectedDate.getDayOfWeek());
        if (currentLogs == null || currentLogs.isEmpty()) {
            assessorFeedback.setText("Cannot evaluate. Please log some exercises first using the + button.");
            return;
        }
        boolean hasChest = false, hasBack = false, hasLegs = false, hasArms = false;
        for (ExerciseData data : currentLogs) {
            String ex = data.name.toLowerCase();
            if (ex.contains("bench") || ex.contains("press") || ex.contains("chest") || ex.contains("push")) hasChest = true;
            if (ex.contains("row") || ex.contains("pull") || ex.contains("lat") || ex.contains("deadlift")) hasBack = true;
            if (ex.contains("squat") || ex.contains("leg") || ex.contains("calf") || ex.contains("lunge")) hasLegs = true;
            if (ex.contains("curl") || ex.contains("tricep") || ex.contains("bicep") || ex.contains("extension")) hasArms = true;
        }
        StringBuilder report = new StringBuilder("SPLIT METRICS:\n");
        int score = 0;
        if (hasChest) { score += 25; report.append("- Push/Chest volume active\n"); }
        if (hasBack) { score += 25; report.append("- Pull/Back volume active\n"); }
        if (hasLegs) { score += 25; report.append("- Leg/Lower Body volume active\n"); }
        if (hasArms) { score += 25; report.append("- Isolation Arm work active\n"); }
        report.append("\nBalance Score: ").append(score).append("%\n");
        report.append(score == 100 ? "Perfect. Split targets all main muscle groups effectively." : score >= 50 ? "Sub-optimal. You are missing core pillars of a balanced split." : "Critical imbalance. Your routine is heavily skewed.");
        assessorFeedback.setText(report.toString());
    }

    private void refreshExerciseListUI() {
        if (exerciseListPanel == null) return;
        exerciseListPanel.removeAll();
        ArrayList<ExerciseData> currentLogs = weeklyRoutine.get(currentSelectedDate.getDayOfWeek());
        if (currentLogs == null || currentLogs.isEmpty()) {
            JLabel emptyLabel = new JLabel("<html>No exercises logged yet.<br>Click + Add Exercise below to add one.</html>");
            emptyLabel.setForeground(Theme.TEXT_MUTED);
            emptyLabel.setFont(Theme.BODY);
            exerciseListPanel.add(emptyLabel);
        } else {
            for (int i = 0; i < currentLogs.size(); i++) {
                final int index = i;
                ExerciseData data = currentLogs.get(i);
                RoundedPanel exerciseCard = new RoundedPanel(15, Theme.CARD_BG);
                exerciseCard.setLayout(new BorderLayout());
                exerciseCard.setMaximumSize(new Dimension(420, 55));
                exerciseCard.setPreferredSize(new Dimension(420, 55));
                exerciseCard.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                JLabel titleLabel = new JLabel(data.name);
                titleLabel.setFont(Theme.BODY_BOLD);
                titleLabel.setForeground(Color.WHITE);
                exerciseCard.add(titleLabel, BorderLayout.WEST);
                if (isEditMode) {
                    JButton deleteCardBtn = new RoundedButton("Delete", 8);
                    deleteCardBtn.setBackground(Theme.DANGER);
                    deleteCardBtn.setForeground(Color.WHITE);
                    deleteCardBtn.setPreferredSize(new Dimension(80, 28));
                    deleteCardBtn.addActionListener(e -> {
                        currentLogs.remove(index);
                        refreshExerciseListUI();
                    });
                    exerciseCard.add(deleteCardBtn, BorderLayout.EAST);
                } else {
                    JLabel statsLabel = new JLabel(data.weight + "  x  " + data.reps);
                    statsLabel.setFont(Theme.SMALL);
                    statsLabel.setForeground(Theme.TEXT_MUTED);
                    exerciseCard.add(statsLabel, BorderLayout.EAST);
                }
                exerciseListPanel.add(exerciseCard);
                exerciseListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        exerciseListPanel.revalidate();
        exerciseListPanel.repaint();
    }

    private void refreshExerciseChoices() {
        Object selected = historyExerciseSelect.getSelectedItem();
        Set<String> names = new LinkedHashSet<>();
        for (ArrayList<ExerciseData> logs : weeklyRoutine.values()) {
            for (ExerciseData data : logs) {
                names.add(data.name);
            }
        }
        historyExerciseSelect.removeAllItems();
        for (String name : names) {
            historyExerciseSelect.addItem(name);
        }
        if (selected != null) {
            historyExerciseSelect.setSelectedItem(selected);
        }
    }

    private void updateHistoryChart() {
        if (historyChart == null || historyExerciseSelect.getSelectedItem() == null) return;
        String exercise = historyExerciseSelect.getSelectedItem().toString();
        LocalDate today = LocalDate.now();
        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            double total = 0;
            for (ExerciseData data : weeklyRoutine.get(date.getDayOfWeek())) {
                if (data.name.equals(exercise)) {
                    total += estimateVolume(data);
                }
            }
            values.add(total);
            labels.add(date.format(DateTimeFormatter.ofPattern("dd")));
        }
        historyChart.setData(values, labels, exercise);
    }

    private double estimateVolume(ExerciseData data) {
        double weight = parseFirstNumber(data.weight);
        String[] parts = data.reps.toLowerCase().split("x");
        double sets = parts.length > 1 ? parseFirstNumber(parts[0]) : 1;
        double reps = parts.length > 1 ? parseFirstNumber(parts[1]) : parseFirstNumber(data.reps);
        return weight * Math.max(1, sets) * Math.max(1, reps);
    }

    private double parseFirstNumber(String text) {
        String cleaned = text == null ? "" : text.replaceAll("[^0-9.]", " ").trim();
        if (cleaned.isEmpty()) return 0;
        try {
            return Double.parseDouble(cleaned.split("\\s+")[0]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void addBodyEntryDialog() {
        JTextField dateField = new JTextField(LocalDate.now().toString());
        JTextField weightField = new JTextField();
        Object[] message = {"Date (YYYY-MM-DD):", dateField, "Bodyweight (kg):", weightField};
        int option = JOptionPane.showConfirmDialog(this, message, "Add Bodyweight", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                bodyEntries.add(new BodyWeightEntry(LocalDate.parse(dateField.getText().trim()), Double.parseDouble(weightField.getText().trim())));
                refreshBodyUI();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid date and weight.");
            }
        }
    }

    private void refreshBodyUI() {
        bodyEntries.sort(Comparator.comparing(entry -> entry.date));
        bodyChart.setData(bodyEntries);
        bodyLogPanel.removeAll();
        double heightMeters = heightCm / 100.0;
        List<BodyWeightEntry> reversed = new ArrayList<>(bodyEntries);
        Collections.reverse(reversed);
        for (BodyWeightEntry entry : reversed) {
            double bmi = entry.weightKg / (heightMeters * heightMeters);
            bodyLogPanel.add(new StatCardPanel(entry.date.toString(), String.format("%.1f kg  |  BMI %.1f", entry.weightKg, bmi), false, value -> {}));
            bodyLogPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        bodyLogPanel.revalidate();
        bodyLogPanel.repaint();
    }

    private void styleScroll(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBackground(Theme.BG_DARK);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GymApp().setVisible(true));
    }

    private static class AppScreen extends JPanel implements Screen {
        private Runnable navigateHook = () -> {};

        AppScreen(BorderLayout layout) {
            super(layout);
        }

        void setNavigateHook(Runnable navigateHook) {
            this.navigateHook = navigateHook;
        }

        @Override
        public void onNavigateTo() {
            navigateHook.run();
        }
    }
}