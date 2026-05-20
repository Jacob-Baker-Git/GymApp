import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JComponent;
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
    private final Map<LocalDate, ArrayList<ExerciseData>> datedWorkoutLogs = new HashMap<>();
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
    private final String[] macroValues = {"", "", "", ""};
    private boolean isMacroEditMode;
    private JPanel macroCardsContainer;
    private JButton macroEditBtn;

    private final String[] prLabels = {"Record 1", "Record 2", "Record 3"};
    private final String[] prValues = {"", "", ""};
    private boolean isPrEditMode;
    private JPanel prCardsContainer;
    private JButton prEditBtn;

    private JComboBox<String> historyExerciseSelect;
    private BarChartPanel historyChart;
    private JPanel bodyLogPanel;
    private LineChartPanel bodyChart;
    private double heightCm = 0;
    private ExerciseData selectedExercise;
    private JLabel exerciseDetailTitle;
    private JLabel exerciseDetailStats;
    private JLabel restTimerLabel;
    private javax.swing.Timer restTimer;
    private int remainingRestSeconds;

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
        addScreen("ASSESSMENT", createAssessmentScreen());
        addScreen("EXERCISE", createExerciseDetailScreen());
        navigateTo("WORKOUT");
    }

    private void seedData() {
        LocalDate today = LocalDate.now();
        currentSelectedDate = today;
        for (int i = -13; i <= 14; i++) {
            timelineDates.add(today.plusDays(i));
        }
    }

    private void addSeedExercise(LocalDate date, ExerciseData data) {
        weeklyRoutine.get(date.getDayOfWeek()).add(data);
        datedWorkoutLogs.computeIfAbsent(date, day -> new ArrayList<>()).add(data);
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
        screen.setOpaque(false);
        screen.setBackground(Theme.BG_DARK);
        screen.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        screen.add(new IconHeader(title, iconType), BorderLayout.NORTH);
        screen.add(createBottomActionBar(title), BorderLayout.SOUTH);
        return screen;
    }

    private JPanel createBottomActionBar(String activeTitle) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel menu = new RoundedPanel(22, Theme.PANEL_DARK);
        menu.setLayout(new GridLayout(6, 1, 0, 5));
        menu.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        menu.setMaximumSize(new Dimension(430, 260));
        menu.setPreferredSize(new Dimension(430, 260));
        menu.setVisible(false);
        menu.add(navMenuButton("WORKOUT", "Lift", activeTitle.equals("Workout"), "workout"));
        menu.add(navMenuButton("MACRO", "Macros", activeTitle.equals("Macros"), "macro"));
        menu.add(navMenuButton("PR", "PRs", activeTitle.equals("Records"), "pr"));
        menu.add(navMenuButton("HISTORY", "History", activeTitle.equals("History"), "history"));
        menu.add(navMenuButton("BODY", "Body", activeTitle.equals("Body"), "body"));
        menu.add(navMenuButton("ASSESSMENT", "Assessment", activeTitle.equals("Assessment"), "assessment"));

        JPanel circles = new JPanel(new BorderLayout());
        circles.setOpaque(false);
        circles.setMaximumSize(new Dimension(430, 66));
        circles.setPreferredSize(new Dimension(430, 66));

        JButton menuButton = circleButton("menu");
        JButton addButton = circleButton("plus");
        menuButton.addActionListener(e -> {
            menu.setVisible(!menu.isVisible());
            wrapper.revalidate();
            wrapper.repaint();
        });
        addButton.addActionListener(e -> addExerciseDialog());
        circles.add(menuButton, BorderLayout.WEST);
        circles.add(addButton, BorderLayout.EAST);

        wrapper.add(menu);
        wrapper.add(Box.createRigidArea(new Dimension(0, 8)));
        wrapper.add(circles);
        return wrapper;
    }

    private JButton navMenuButton(String target, String label, boolean active, String iconType) {
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
                drawMiniIcon(g2, iconType, 14, getHeight() / 2 - 10);
                g2.setFont(Theme.SMALL_BOLD);
                g2.drawString(label, 46, getHeight() / 2 + 5);
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

    private JButton circleButton(String type) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(Theme.verticalGradient(Theme.ACCENT, getWidth(), getHeight()));
                g2.fillOval(4, 4, getWidth() - 8, getHeight() - 8);
                g2.setColor(Theme.ACCENT_GLOW);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(3, 3, getWidth() - 7, getHeight() - 7);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                if ("plus".equals(type)) {
                    g2.drawLine(cx - 11, cy, cx + 11, cy);
                    g2.drawLine(cx, cy - 11, cx, cy + 11);
                } else {
                    g2.drawLine(cx - 12, cy - 8, cx + 12, cy - 8);
                    g2.drawLine(cx - 12, cy, cx + 12, cy);
                    g2.drawLine(cx - 12, cy + 8, cx + 12, cy + 8);
                }
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(58, 58));
        button.setMinimumSize(new Dimension(58, 58));
        button.setMaximumSize(new Dimension(58, 58));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
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
        } else if ("assessment".equals(type)) {
            g2.drawOval(x + 2, y + 2, 16, 16);
            g2.drawLine(x + 10, y + 5, x + 10, y + 11);
            g2.drawLine(x + 10, y + 11, x + 15, y + 8);
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
        content.setOpaque(false);

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
        exerciseListPanel.setOpaque(false);
        JScrollPane listScroll = new JScrollPane(exerciseListPanel);
        listScroll.setPreferredSize(new Dimension(420, 220));
        styleScroll(listScroll);
        content.add(listScroll);

        mainWrapper.add(content, BorderLayout.CENTER);
        mainWrapper.setNavigateHook(this::refreshExerciseListUI);
        wireWorkoutActions();
        return mainWrapper;
    }

    private JComponent createTimelinePanel() {
        JPanel calendarPanel = new JPanel();
        calendarPanel.setLayout(new BoxLayout(calendarPanel, BoxLayout.X_AXIS));
        calendarPanel.setOpaque(false);
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
        calendarScroll.getViewport().addMouseListener(dragScrollListener);
        calendarScroll.getViewport().addMouseMotionListener(dragScrollListener);
        for (java.awt.Component component : calendarPanel.getComponents()) {
            component.addMouseListener(dragScrollListener);
            component.addMouseMotionListener(dragScrollListener);
        }
        return calendarScroll;
    }

    private void wireWorkoutActions() {
        globalEditBtn.addActionListener(e -> {
            isEditMode = !isEditMode;
            globalEditBtn.setText(isEditMode ? "Done" : "Edit Logs");
            globalEditBtn.setBackground(isEditMode ? Theme.DANGER : Theme.PANEL_MID);
            globalEditBtn.setForeground(isEditMode ? Color.WHITE : Theme.TEXT_MUTED);
            refreshExerciseListUI();
        });
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
        cards.setOpaque(false);
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
        JButton heightBtn = new RoundedButton("Set Height", 12);
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
        bodyLogPanel.setOpaque(false);
        JScrollPane logScroll = new JScrollPane(bodyLogPanel);
        logScroll.setPreferredSize(new Dimension(420, 160));
        styleScroll(logScroll);
        center.add(logScroll, BorderLayout.SOUTH);
        screen.add(center, BorderLayout.CENTER);
        heightBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Height in cm:", heightCm > 0 ? heightCm : "");
            if (input != null) {
                try {
                    heightCm = Double.parseDouble(input.trim());
                    heightBtn.setText(heightCm > 0 ? "Height: " + (int) heightCm + " cm" : "Set Height");
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

    private JPanel createAssessmentScreen() {
        AppScreen screen = createScreenShell("Assessment", "assessment");
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(Box.createRigidArea(new Dimension(0, 70)));

        JButton assessBtn = new RoundedButton("Run Weekly Split Assessment", 15);
        assessBtn.setBackground(Theme.ACCENT);
        assessBtn.setForeground(Color.WHITE);
        assessBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        assessBtn.setMaximumSize(new Dimension(330, 46));
        assessBtn.setPreferredSize(new Dimension(330, 46));
        assessBtn.addActionListener(e -> runAssessment());
        center.add(assessBtn);

        center.add(Box.createRigidArea(new Dimension(0, 38)));

        RoundedPanel reportCard = new RoundedPanel(20, Theme.CARD_BG);
        reportCard.setLayout(new BorderLayout());
        reportCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        reportCard.setMaximumSize(new Dimension(430, 260));
        reportCard.setPreferredSize(new Dimension(430, 260));

        assessorFeedback = new JTextArea("Assessment Report will appear here...");
        assessorFeedback.setEditable(false);
        assessorFeedback.setLineWrap(true);
        assessorFeedback.setWrapStyleWord(true);
        assessorFeedback.setOpaque(false);
        assessorFeedback.setForeground(Theme.TEXT_MUTED);
        assessorFeedback.setFont(Theme.BODY);
        reportCard.add(assessorFeedback, BorderLayout.CENTER);

        center.add(reportCard);
        screen.add(center, BorderLayout.CENTER);
        return screen;
    }

    private JPanel createExerciseDetailScreen() {
        AppScreen screen = createScreenShell("Exercise", "workout");
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        RoundedPanel detailCard = new RoundedPanel(20, Theme.CARD_BG);
        detailCard.setLayout(new BoxLayout(detailCard, BoxLayout.Y_AXIS));
        detailCard.setBorder(BorderFactory.createEmptyBorder(24, 22, 24, 22));
        detailCard.setMaximumSize(new Dimension(430, 250));
        detailCard.setPreferredSize(new Dimension(430, 250));

        exerciseDetailTitle = new JLabel("Select an exercise");
        exerciseDetailTitle.setForeground(Theme.TEXT);
        exerciseDetailTitle.setFont(Theme.TITLE_FONT);
        exerciseDetailTitle.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        exerciseDetailStats = new JLabel("Open an exercise from the workout list.");
        exerciseDetailStats.setForeground(Theme.TEXT_MUTED);
        exerciseDetailStats.setFont(Theme.BODY);
        exerciseDetailStats.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        restTimerLabel = new JLabel("Rest timer ready");
        restTimerLabel.setForeground(Theme.ACCENT_2);
        restTimerLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 30));
        restTimerLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        JButton logSetButton = new RoundedButton("Log Set", 16);
        logSetButton.setBackground(Theme.ACCENT);
        logSetButton.setForeground(Color.WHITE);
        logSetButton.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        logSetButton.setMaximumSize(new Dimension(230, 46));
        logSetButton.setPreferredSize(new Dimension(230, 46));
        logSetButton.addActionListener(e -> logSetAndStartRest());

        JButton backButton = new RoundedButton("Back to Workout", 14);
        backButton.setBackground(Theme.PANEL_MID);
        backButton.setForeground(Theme.TEXT);
        backButton.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(230, 42));
        backButton.addActionListener(e -> navigateTo("WORKOUT"));

        detailCard.add(exerciseDetailTitle);
        detailCard.add(Box.createRigidArea(new Dimension(0, 12)));
        detailCard.add(exerciseDetailStats);
        detailCard.add(Box.createRigidArea(new Dimension(0, 24)));
        detailCard.add(restTimerLabel);
        detailCard.add(Box.createRigidArea(new Dimension(0, 18)));
        detailCard.add(logSetButton);

        center.add(Box.createRigidArea(new Dimension(0, 30)));
        center.add(detailCard);
        center.add(Box.createRigidArea(new Dimension(0, 18)));
        center.add(backButton);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(this::refreshExerciseDetail);
        return screen;
    }

    private void refreshExerciseDetail() {
        if (selectedExercise == null) {
            exerciseDetailTitle.setText("Select an exercise");
            exerciseDetailStats.setText("Open an exercise from the workout list.");
            restTimerLabel.setText("Rest timer ready");
            return;
        }
        exerciseDetailTitle.setText(selectedExercise.name);
        exerciseDetailStats.setText(formatExerciseSummary(selectedExercise)
                + "  |  Sets logged " + selectedExercise.loggedSets);
        if (remainingRestSeconds <= 0) {
            restTimerLabel.setText("Rest timer ready");
        }
    }

    private void logSetAndStartRest() {
        if (selectedExercise == null) {
            return;
        }
        selectedExercise.loggedSets++;
        remainingRestSeconds = selectedExercise.restSeconds;
        if (restTimer != null) {
            restTimer.stop();
        }
        restTimer = new javax.swing.Timer(1000, e -> {
            remainingRestSeconds--;
            updateRestTimerLabel();
            if (remainingRestSeconds <= 0) {
                restTimer.stop();
                restTimerLabel.setText("Rest complete");
            }
        });
        refreshExerciseDetail();
        updateRestTimerLabel();
        restTimer.start();
    }

    private void updateRestTimerLabel() {
        int minutes = Math.max(0, remainingRestSeconds) / 60;
        int seconds = Math.max(0, remainingRestSeconds) % 60;
        restTimerLabel.setText(String.format("Rest %d:%02d", minutes, seconds));
    }

    private void addExerciseDialog() {
        String exerciseName = JOptionPane.showInputDialog(this, "Exercise name:", "Add Exercise", JOptionPane.PLAIN_MESSAGE);
        if (exerciseName == null || exerciseName.trim().isEmpty()) {
            return;
        }

        JTextField weightField = new JTextField();
        JTextField setsField = new JTextField("3");
        JTextField repsField = new JTextField("10");
        JTextField restField = new JTextField("90");

        JPanel detailsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        detailsPanel.add(new JLabel("Weight:"));
        detailsPanel.add(weightField);
        detailsPanel.add(new JLabel("Sets:"));
        detailsPanel.add(setsField);
        detailsPanel.add(new JLabel("Reps:"));
        detailsPanel.add(repsField);
        detailsPanel.add(new JLabel("Rest Time (seconds):"));
        detailsPanel.add(restField);

        Object[] message = {
                "Add details for " + exerciseName.trim(),
                detailsPanel
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Exercise Details", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            int sets = parsePositiveInt(setsField.getText(), 3, 1);
            int reps = parsePositiveInt(repsField.getText(), 10, 1);
            int restSeconds = parsePositiveInt(restField.getText(), 90, 5);
            ExerciseData data = new ExerciseData(exerciseName.trim(), weightField.getText().trim(), sets + "x" + reps, restSeconds);
            weeklyRoutine.get(currentSelectedDate.getDayOfWeek()).add(data);
            datedWorkoutLogs.computeIfAbsent(currentSelectedDate, day -> new ArrayList<>()).add(data);
            refreshExerciseListUI();
        }
    }

    private int parsePositiveInt(String text, int fallback, int minimum) {
        try {
            return Math.max(minimum, Integer.parseInt(text.trim()));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String formatExerciseSummary(ExerciseData data) {
        String[] setRepParts = data.reps == null ? new String[0] : data.reps.toLowerCase().split("x");
        String sets = setRepParts.length > 0 && !setRepParts[0].trim().isEmpty() ? setRepParts[0].trim() : "?";
        String reps = setRepParts.length > 1 && !setRepParts[1].trim().isEmpty() ? setRepParts[1].trim() : "?";
        String weight = data.weight == null || data.weight.trim().isEmpty() ? "No weight" : data.weight.trim();
        return weight + "  |  " + sets + " sets x " + reps + " reps  |  Rest " + data.restSeconds + "s";
    }

    private String formatExerciseSummaryCompact(ExerciseData data) {
        String weight = data.weight == null || data.weight.trim().isEmpty() ? "-" : data.weight.trim();
        String reps = data.reps == null || data.reps.trim().isEmpty() ? "?x?" : data.reps.trim();
        return weight + "  |  " + reps + "  |  " + data.restSeconds + "s";
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
            exerciseListPanel.add(createEmptyStateCard("No exercises yet", "Tap the + button to build today's session."));
        } else {
            for (int i = 0; i < currentLogs.size(); i++) {
                final int index = i;
                ExerciseData data = currentLogs.get(i);
                RoundedPanel exerciseCard = new RoundedPanel(15, Theme.CARD_BG);
                exerciseCard.setLayout(new BorderLayout());
                exerciseCard.setMaximumSize(new Dimension(420, 55));
                exerciseCard.setPreferredSize(new Dimension(420, 55));
                exerciseCard.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                exerciseCard.setToolTipText("Open exercise");
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
                        ArrayList<ExerciseData> datedLogs = datedWorkoutLogs.get(currentSelectedDate);
                        if (datedLogs != null) {
                            datedLogs.remove(data);
                        }
                        currentLogs.remove(index);
                        refreshExerciseListUI();
                    });
                    exerciseCard.add(deleteCardBtn, BorderLayout.EAST);
                } else {
                    JLabel statsLabel = new JLabel(formatExerciseSummaryCompact(data));
                    statsLabel.setFont(Theme.SMALL);
                    statsLabel.setForeground(Theme.TEXT_MUTED);
                    exerciseCard.add(statsLabel, BorderLayout.EAST);
                    exerciseCard.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            selectedExercise = data;
                            navigateTo("EXERCISE");
                        }
                    });
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
        for (ArrayList<ExerciseData> logs : datedWorkoutLogs.values()) {
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
            for (ExerciseData data : datedWorkoutLogs.getOrDefault(date, new ArrayList<>())) {
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
            String value = String.format("%.1f kg", entry.weightKg);
            if (heightMeters > 0) {
                double bmi = entry.weightKg / (heightMeters * heightMeters);
                value += String.format("  |  BMI %.1f", bmi);
            } else {
                value += "  |  Set height for BMI";
            }
            bodyLogPanel.add(new StatCardPanel(entry.date.toString(), value, false, ignored -> {}));
            bodyLogPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        bodyLogPanel.revalidate();
        bodyLogPanel.repaint();
    }

    private void styleScroll(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBackground(Theme.BG_DARK);
    }

    private JPanel createEmptyStateCard(String title, String message) {
        RoundedPanel card = new RoundedPanel(20, Theme.PANEL_DARK);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 18, 24, 18));
        card.setMaximumSize(new Dimension(420, 140));
        card.setPreferredSize(new Dimension(420, 140));

        JLabel icon = new JLabel("+", SwingConstants.CENTER);
        icon.setForeground(Theme.ACCENT);
        icon.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 30));
        icon.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setFont(Theme.HEADER_FONT);
        titleLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setForeground(Theme.TEXT_MUTED);
        messageLabel.setFont(Theme.BODY);
        messageLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(messageLabel);
        return card;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GymApp().setVisible(true));
    }

    private static class AppScreen extends JPanel implements Screen {
        private Runnable navigateHook = () -> {};

        AppScreen(BorderLayout layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(Theme.verticalGradient(Theme.BG_TOP, getWidth(), getHeight()));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 10));
            for (int y = 92; y < getHeight(); y += 82) {
                g2.drawLine(-30, y, getWidth() + 30, y - 42);
            }
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 18));
            g2.fillRoundRect(18, 74, getWidth() - 36, 6, 8, 8);
            g2.dispose();
            super.paintComponent(g);
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
