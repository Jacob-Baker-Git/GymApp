import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
    private final Map<LocalDate, Map<ExerciseData, Integer>> loggedSetsByDate = new HashMap<>();
    private final ArrayList<LocalDate> timelineDates = new ArrayList<>();
    private final Map<String, Screen> screens = new HashMap<>();
    private final ArrayList<BodyWeightEntry> bodyEntries = new ArrayList<>();

    private LocalDate currentSelectedDate;
    private CardLayout cardLayout;
    private JPanel containerPanel;
    private JPanel exerciseListPanel;
    private JTextArea assessorFeedback;
    private boolean isEditMode;
    private String activeScreenName = "WORKOUT";
    private JButton globalEditBtn;
    private RoundedPanel selectedDayBox;
    private Point dragStartPoint;
    private JPanel openMenuPanel;
    private JPanel openMenuWrapper;
    private JButton openMenuButton;

    private final String[] macroLabels = {"Calories", "Protein", "Carbs", "Fats"};
    private final String[] macroGoalValues = {"", "", "", ""};
    private final String[] macroCurrentValues = {"", "", "", ""};
    private boolean isMacroEditMode;
    private JPanel macroCardsContainer;
    private JButton macroEditBtn;

    private final ArrayList<RecordData> personalRecords = new ArrayList<>();
    private boolean isPrEditMode;
    private JPanel prCardsContainer;
    private JButton prEditBtn;

    private JComboBox<String> historyExerciseSelect;
    private BarChartPanel historyChart;
    private JPanel bodyLogPanel;
    private LineChartPanel bodyChart;
    private JButton heightSummaryButton;
    private double heightCm = 0;
    private ExerciseData selectedExercise;
    private JLabel exerciseDetailTitle;
    private JLabel exerciseDetailStats;
    private JLabel restTimerLabel;
    private javax.swing.Timer restTimer;
    private int remainingRestSeconds;

    public GymApp() {
        setTitle("IronPulse Gym Tracker");
        setIconImage(createAppIcon());
        setSize(480, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        setContentPane(containerPanel);
        installMenuDismissListener();

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

    private Image createAppIcon() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK, size, size));
        g2.fillRoundRect(3, 3, size - 6, size - 6, 18, 18);
        g2.setColor(Theme.ACCENT_GLOW);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(4, 4, size - 9, size - 9, 18, 18);

        g2.setPaint(Theme.verticalGradient(Theme.ACCENT, size, size));
        g2.fillRoundRect(15, 29, 34, 7, 7, 7);
        g2.fillRoundRect(9, 22, 10, 21, 7, 7);
        g2.fillRoundRect(45, 22, 10, 21, 7, 7);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(25, 45, 31, 18);
        g2.drawLine(31, 18, 39, 36);
        g2.drawLine(39, 36, 46, 17);
        g2.dispose();
        return image;
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
    }

    private void addScreen(String name, JPanel screen) {
        containerPanel.add(screen, name);
        if (screen instanceof Screen) {
            screens.put(name, (Screen) screen);
        }
    }

    private void navigateTo(String name) {
        closeOpenMenu();
        activeScreenName = name;
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
        menuButton.addActionListener(e -> {
            if (openMenuPanel != null && openMenuPanel != menu) {
                closeOpenMenu();
            }
            menu.setVisible(!menu.isVisible());
            openMenuPanel = menu.isVisible() ? menu : null;
            openMenuWrapper = menu.isVisible() ? wrapper : null;
            openMenuButton = menu.isVisible() ? menuButton : null;
            wrapper.revalidate();
            wrapper.repaint();
        });
        circles.add(menuButton, BorderLayout.WEST);
        if (hasPlusAction(activeTitle)) {
            JButton addButton = circleButton("plus");
            addButton.addActionListener(e -> handlePlusAction());
            circles.add(addButton, BorderLayout.EAST);
        }

        wrapper.add(menu);
        wrapper.add(Box.createRigidArea(new Dimension(0, 8)));
        wrapper.add(circles);
        return wrapper;
    }

    private boolean hasPlusAction(String activeTitle) {
        return "Workout".equals(activeTitle) || "Body".equals(activeTitle) || "Records".equals(activeTitle);
    }

    private void closeOpenMenu() {
        if (openMenuPanel != null) {
            openMenuPanel.setVisible(false);
        }
        if (openMenuWrapper != null) {
            openMenuWrapper.revalidate();
            openMenuWrapper.repaint();
        }
        openMenuPanel = null;
        openMenuWrapper = null;
        openMenuButton = null;
    }

    private void installMenuDismissListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) {
                return;
            }
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED || openMenuPanel == null || !openMenuPanel.isVisible()) {
                return;
            }
            Component source = mouseEvent.getComponent();
            boolean clickedMenu = source != null && SwingUtilities.isDescendingFrom(source, openMenuPanel);
            boolean clickedMenuButton = source != null && openMenuButton != null && SwingUtilities.isDescendingFrom(source, openMenuButton);
            if (!clickedMenu && !clickedMenuButton) {
                closeOpenMenu();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void handlePlusAction() {
        if ("PR".equals(activeScreenName)) {
            addRecordDialog();
        } else if ("BODY".equals(activeScreenName)) {
            addBodyEntryDialog();
        } else {
            addExerciseDialog();
        }
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
                g2.setColor(active ? Theme.ACCENT : Theme.TEXT_MUTED);
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
                int diameter = Math.min(getWidth(), getHeight()) - 8;
                int x = (getWidth() - diameter) / 2;
                int y = (getHeight() - diameter) / 2;
                g2.setPaint(Theme.verticalGradient(Theme.ACCENT, getWidth(), getHeight()));
                g2.fillOval(x, y, diameter, diameter);
                g2.setColor(Theme.ACCENT_GLOW);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x - 1, y - 1, diameter + 1, diameter + 1);
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
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, 22, 22));
        g2.fillRoundRect(x - 2, y - 2, 24, 24, 9, 9);
        g2.setColor(new Color(255, 255, 255, 210));
        if ("workout".equals(type)) {
            g2.drawLine(x, y + 10, x + 20, y + 10);
            g2.drawLine(x + 3, y + 5, x + 3, y + 15);
            g2.drawLine(x + 17, y + 5, x + 17, y + 15);
        } else if ("macro".equals(type)) {
            g2.drawOval(x + 2, y + 2, 16, 16);
            g2.drawLine(x + 10, y + 9, x + 17, y + 3);
        } else if ("pr".equals(type)) {
            g2.drawLine(x + 10, y + 2, x + 10, y + 17);
            g2.drawLine(x + 4, y + 8, x + 10, y + 2);
            g2.drawLine(x + 16, y + 8, x + 10, y + 2);
        } else if ("history".equals(type)) {
            g2.fillRoundRect(x + 2, y + 11, 4, 7, 3, 3);
            g2.fillRoundRect(x + 8, y + 7, 4, 11, 3, 3);
            g2.fillRoundRect(x + 14, y + 3, 4, 15, 3, 3);
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
        AppScreen mainWrapper = new AppScreen(new BorderLayout());
        mainWrapper.setOpaque(false);
        mainWrapper.setBackground(Theme.BG_DARK);
        mainWrapper.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));

        JPanel fixedTop = new JPanel();
        fixedTop.setOpaque(false);
        fixedTop.setLayout(new BoxLayout(fixedTop, BoxLayout.Y_AXIS));
        fixedTop.add(new IconHeader("Workout", "workout"));
        fixedTop.add(Box.createRigidArea(new Dimension(0, 8)));
        fixedTop.add(createTimelinePanel());
        fixedTop.add(Box.createRigidArea(new Dimension(0, 12)));
        mainWrapper.add(fixedTop, BorderLayout.NORTH);
        mainWrapper.add(createBottomActionBar("Workout"), BorderLayout.SOUTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel targetHeaderPanel = new JPanel(new BorderLayout());
        targetHeaderPanel.setOpaque(false);
        targetHeaderPanel.setMaximumSize(new Dimension(440, 36));
        targetHeaderPanel.setPreferredSize(new Dimension(440, 36));
        JLabel logHeader = new JLabel("Today's Targets:");
        logHeader.setForeground(Theme.TEXT);
        logHeader.setFont(Theme.HEADER_FONT);
        targetHeaderPanel.add(logHeader, BorderLayout.WEST);
        globalEditBtn = new RoundedButton("Edit Logs", 10);
        globalEditBtn.setBackground(Theme.ACCENT);
        globalEditBtn.setForeground(Color.WHITE);
        globalEditBtn.setPreferredSize(new Dimension(100, 30));
        globalEditBtn.setMaximumSize(new Dimension(120, 30));
        targetHeaderPanel.add(globalEditBtn, BorderLayout.EAST);
        content.add(targetHeaderPanel);
        content.add(Box.createRigidArea(new Dimension(0, 12)));

        exerciseListPanel = new JPanel();
        exerciseListPanel.setLayout(new BoxLayout(exerciseListPanel, BoxLayout.Y_AXIS));
        exerciseListPanel.setOpaque(false);
        JScrollPane listScroll = new JScrollPane(exerciseListPanel);
        listScroll.setPreferredSize(new Dimension(420, 220));
        styleScroll(listScroll);
        enableVerticalDragScroll(listScroll);
        content.add(createLiftColumnHeader());
        content.add(Box.createRigidArea(new Dimension(0, 6)));
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
            RoundedPanel dayBox = new RoundedPanel(15, Theme.PANEL_DARK);
            dayBox.putClientProperty("date", date);
            dayBox.setLayout(new GridLayout(3, 1));
            dayBox.setPreferredSize(new Dimension(68, 74));
            dayBox.setMaximumSize(new Dimension(68, 74));
            if (date.equals(currentSelectedDate)) {
                selectedDayBox = dayBox;
            }
            JLabel dayLabel = new JLabel(date.format(dayFormatter), SwingConstants.CENTER);
            JLabel monthLabel = new JLabel(date.getMonth().getDisplayName(TextStyle.SHORT, java.util.Locale.ENGLISH), SwingConstants.CENTER);
            JLabel numLabel = new JLabel(date.format(numFormatter), SwingConstants.CENTER);
            dayLabel.setForeground(Theme.TEXT_MUTED);
            monthLabel.setForeground(Theme.shift(Theme.TEXT_MUTED, -20));
            monthLabel.setFont(Theme.SMALL);
            monthLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));
            numLabel.setForeground(Theme.TEXT);
            numLabel.setFont(Theme.HEADER_FONT);
            dayBox.add(dayLabel);
            dayBox.add(monthLabel);
            dayBox.add(numLabel);
            applyDateBoxState(dayBox, date, date.equals(currentSelectedDate));
            dayBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (selectedDayBox != null) {
                        LocalDate previousDate = (LocalDate) selectedDayBox.getClientProperty("date");
                        applyDateBoxState(selectedDayBox, previousDate, false);
                    }
                    selectedDayBox = dayBox;
                    currentSelectedDate = date;
                    applyDateBoxState(dayBox, date, true);
                    refreshExerciseListUI();
                }
            });
            calendarPanel.add(dayBox);
            calendarPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        }
        JScrollPane calendarScroll = new JScrollPane(calendarPanel);
        calendarScroll.setPreferredSize(new Dimension(420, 88));
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
        SwingUtilities.invokeLater(() -> {
            if (selectedDayBox != null) {
                int x = Math.max(0, selectedDayBox.getX() - 16);
                calendarScroll.getViewport().setViewPosition(new Point(x, 0));
            }
        });
        return calendarScroll;
    }

    private void applyDateBoxState(RoundedPanel dayBox, LocalDate date, boolean selected) {
        boolean today = date.equals(LocalDate.now());
        if (selected) {
            dayBox.setBackgroundColor(Theme.CARD_BG_SELECTED);
            dayBox.setSelectedGlow(true);
        } else if (today) {
            dayBox.setBackgroundColor(Theme.shift(Theme.ACCENT_DARK, -10));
            dayBox.setSelectedGlow(true);
        } else {
            dayBox.setBackgroundColor(Theme.PANEL_DARK);
            dayBox.setSelectedGlow(false);
        }
    }

    private JPanel createLiftColumnHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(420, 20));
        JLabel left = new JLabel("Exercise");
        left.setForeground(Theme.TEXT_MUTED);
        left.setFont(Theme.SMALL_BOLD);
        JLabel right = new JLabel("Weight | Sets x Reps | Rest");
        right.setForeground(Theme.TEXT_MUTED);
        right.setFont(Theme.SMALL_BOLD);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void wireWorkoutActions() {
        globalEditBtn.addActionListener(e -> {
            isEditMode = !isEditMode;
            globalEditBtn.setText(isEditMode ? "Done" : "Edit Logs");
            globalEditBtn.setBackground(isEditMode ? Theme.DANGER : Theme.ACCENT);
            globalEditBtn.setForeground(Color.WHITE);
            refreshExerciseListUI();
        });
    }

    private JPanel createMacroTrackerScreen() {
        AppScreen panel = createScreenShell("Macros", "macro");
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        macroEditBtn = new RoundedButton("Edit Stats", 10);
        macroEditBtn.setPreferredSize(new Dimension(110, 32));
        JPanel tools = new JPanel(new BorderLayout());
        tools.setOpaque(false);
        tools.add(macroEditBtn, BorderLayout.EAST);
        center.add(tools, BorderLayout.NORTH);

        macroCardsContainer = new JPanel();
        macroCardsContainer.setLayout(new BoxLayout(macroCardsContainer, BoxLayout.Y_AXIS));
        macroCardsContainer.setOpaque(false);
        JScrollPane scroll = new JScrollPane(macroCardsContainer);
        styleScroll(scroll);
        enableVerticalDragScroll(scroll);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        macroEditBtn.addActionListener(e -> {
            isMacroEditMode = !isMacroEditMode;
            updateEditButton(macroEditBtn, isMacroEditMode);
            refreshMacroCards();
        });
        panel.setNavigateHook(this::refreshMacroCards);
        refreshMacroCards();
        return panel;
    }

    private JPanel createPersonalRecordsScreen() {
        AppScreen panel = createScreenShell("Records", "pr");
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        JPanel tools = new JPanel(new BorderLayout(8, 0));
        tools.setOpaque(false);
        prEditBtn = new RoundedButton("Edit", 10);
        prEditBtn.setPreferredSize(new Dimension(90, 32));
        tools.add(prEditBtn, BorderLayout.EAST);
        center.add(tools, BorderLayout.NORTH);

        prCardsContainer = new JPanel();
        prCardsContainer.setLayout(new BoxLayout(prCardsContainer, BoxLayout.Y_AXIS));
        prCardsContainer.setOpaque(false);
        JScrollPane scroll = new JScrollPane(prCardsContainer);
        styleScroll(scroll);
        enableVerticalDragScroll(scroll);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        prEditBtn.addActionListener(e -> {
            isPrEditMode = !isPrEditMode;
            prEditBtn.setText(isPrEditMode ? "Done" : "Edit");
            prEditBtn.setBackground(isPrEditMode ? Theme.DANGER : Theme.PANEL_MID);
            prEditBtn.setForeground(isPrEditMode ? Color.WHITE : Theme.TEXT_MUTED);
            refreshRecordCards();
        });
        panel.setNavigateHook(this::refreshRecordCards);
        refreshRecordCards();
        return panel;
    }

    private void refreshMacroCards() {
        macroCardsContainer.removeAll();
        macroCardsContainer.add(Box.createRigidArea(new Dimension(0, 18)));
        addSectionLabel(macroCardsContainer, "Macro Goals");
        addStatCards(macroCardsContainer, macroLabels, macroGoalValues, isMacroEditMode);
        macroCardsContainer.add(Box.createRigidArea(new Dimension(0, 16)));
        addSectionLabel(macroCardsContainer, "Current Macros");
        addStatCards(macroCardsContainer, macroLabels, macroCurrentValues, isMacroEditMode);
        macroCardsContainer.revalidate();
        macroCardsContainer.repaint();
    }

    private void addSectionLabel(JPanel container, String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.HEADER_FONT);
        label.setForeground(Theme.TEXT);
        label.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        container.add(label);
        container.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private void addStatCards(JPanel container, String[] labels, String[] values, boolean editMode) {
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            container.add(new StatCardPanel(labels[index], values[index], editMode, value -> values[index] = value));
            container.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private void refreshRecordCards() {
        prCardsContainer.removeAll();
        prCardsContainer.add(Box.createRigidArea(new Dimension(0, 18)));
        if (personalRecords.isEmpty()) {
            prCardsContainer.add(createEmptyStateCard("No records yet", "Add a lift name and weight to start your PR board."));
        } else {
            for (int i = 0; i < personalRecords.size(); i++) {
                final int index = i;
                RecordData record = personalRecords.get(i);
                RoundedPanel card = new RoundedPanel(15, Theme.CARD_BG);
                card.setLayout(new BorderLayout());
                card.setMaximumSize(new Dimension(440, 58));
                card.setPreferredSize(new Dimension(440, 58));
                card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

                JLabel nameLabel = new JLabel(record.name);
                nameLabel.setForeground(Theme.TEXT);
                nameLabel.setFont(Theme.BODY_BOLD);
                card.add(nameLabel, BorderLayout.WEST);

                if (isPrEditMode) {
                    JButton deleteBtn = new RoundedButton("Delete", 8);
                    deleteBtn.setBackground(Theme.DANGER);
                    deleteBtn.setForeground(Color.WHITE);
                    deleteBtn.setPreferredSize(new Dimension(82, 28));
                    deleteBtn.addActionListener(e -> {
                        personalRecords.remove(index);
                        refreshRecordCards();
                    });
                    card.add(deleteBtn, BorderLayout.EAST);
                } else {
                    JLabel weightLabel = new JLabel(record.weight);
                    weightLabel.setForeground(Theme.TEXT_MUTED);
                    weightLabel.setFont(Theme.BODY);
                    card.add(weightLabel, BorderLayout.EAST);
                }
                prCardsContainer.add(card);
                prCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        prCardsContainer.revalidate();
        prCardsContainer.repaint();
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
        enableVerticalDragScroll(scroll);
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

        RoundedPanel heightBox = new RoundedPanel(16, Theme.PANEL_DARK);
        heightBox.setLayout(new BorderLayout());
        heightBox.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        heightBox.setPreferredSize(new Dimension(420, 58));
        JLabel heightTitle = new JLabel("Height");
        heightTitle.setForeground(Theme.TEXT);
        heightTitle.setFont(Theme.BODY_BOLD);
        heightSummaryButton = new RoundedButton("Set height", 10);
        heightSummaryButton.setBackground(Theme.ACCENT);
        heightSummaryButton.setForeground(Color.WHITE);
        heightSummaryButton.setPreferredSize(new Dimension(112, 30));
        heightSummaryButton.setToolTipText("Click to set height");
        heightSummaryButton.addActionListener(e -> setHeightDialog());
        heightBox.add(heightTitle, BorderLayout.WEST);
        heightBox.add(heightSummaryButton, BorderLayout.EAST);
        center.add(heightBox, BorderLayout.NORTH);

        bodyChart = new LineChartPanel();
        center.add(bodyChart, BorderLayout.CENTER);
        bodyLogPanel = new JPanel();
        bodyLogPanel.setLayout(new BoxLayout(bodyLogPanel, BoxLayout.Y_AXIS));
        bodyLogPanel.setOpaque(false);
        JScrollPane logScroll = new JScrollPane(bodyLogPanel);
        logScroll.setPreferredSize(new Dimension(420, 160));
        styleScroll(logScroll);
        enableVerticalDragScroll(logScroll);
        center.add(logScroll, BorderLayout.SOUTH);
        screen.add(center, BorderLayout.CENTER);
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
                + "  |  Sets logged " + getLoggedSets(selectedExercise));
        if (remainingRestSeconds <= 0) {
            restTimerLabel.setText("Rest timer ready");
        }
    }

    private void logSetAndStartRest() {
        if (selectedExercise == null) {
            return;
        }
        int targetSets = getTargetSets(selectedExercise);
        int loggedSets = getLoggedSets(selectedExercise);
        if (loggedSets >= targetSets) {
            JOptionPane.showMessageDialog(this, "Exercise complete. You have logged all " + targetSets + " sets.");
            return;
        }
        loggedSets++;
        setLoggedSets(selectedExercise, loggedSets);
        if (loggedSets >= targetSets) {
            if (restTimer != null) {
                restTimer.stop();
            }
            remainingRestSeconds = 0;
            setExerciseHistoryComplete(selectedExercise, true);
            refreshExerciseDetail();
            JOptionPane.showMessageDialog(this, selectedExercise.name + " complete. Nice work.");
            return;
        }
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

    private int getTargetSets(ExerciseData data) {
        String[] setRepParts = data.reps == null ? new String[0] : data.reps.toLowerCase().split("x");
        if (setRepParts.length == 0) {
            return 1;
        }
        return parsePositiveInt(setRepParts[0], 1, 1);
    }

    private int getLoggedSets(ExerciseData data) {
        Map<ExerciseData, Integer> daySets = loggedSetsByDate.get(currentSelectedDate);
        return daySets == null ? 0 : daySets.getOrDefault(data, 0);
    }

    private void setLoggedSets(ExerciseData data, int sets) {
        loggedSetsByDate.computeIfAbsent(currentSelectedDate, day -> new HashMap<>()).put(data, sets);
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
            ExerciseData data = new ExerciseData(exerciseName.trim(), cleanNumberText(weightField.getText()), sets + "x" + reps, restSeconds);
            weeklyRoutine.get(currentSelectedDate.getDayOfWeek()).add(data);
            refreshExerciseListUI();
        }
    }

    private void addRecordDialog() {
        JTextField nameField = new JTextField();
        JTextField weightField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        panel.add(new JLabel("Record Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Weight:"));
        panel.add(weightField);

        int option = JOptionPane.showConfirmDialog(this, panel, "Add Personal Record", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            personalRecords.add(new RecordData(nameField.getText().trim(), weightField.getText().trim()));
            refreshRecordCards();
        }
    }

    private int parsePositiveInt(String text, int fallback, int minimum) {
        try {
            return Math.max(minimum, Integer.parseInt(text.trim()));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String cleanNumberText(String text) {
        return text == null ? "" : text.replaceAll("[^0-9.]", "").trim();
    }

    private String formatExerciseSummary(ExerciseData data) {
        String[] setRepParts = data.reps == null ? new String[0] : data.reps.toLowerCase().split("x");
        String sets = setRepParts.length > 0 && !setRepParts[0].trim().isEmpty() ? setRepParts[0].trim() : "?";
        String reps = setRepParts.length > 1 && !setRepParts[1].trim().isEmpty() ? setRepParts[1].trim() : "?";
        return formatWeight(data) + "  |  " + sets + " sets x " + reps + " reps  |  Rest " + data.restSeconds + "s";
    }

    private String formatExerciseSummaryCompact(ExerciseData data) {
        String reps = data.reps == null || data.reps.trim().isEmpty() ? "?x?" : data.reps.trim();
        return formatWeight(data) + " | " + reps + " | " + data.restSeconds + "s";
    }

    private String formatWeight(ExerciseData data) {
        if (data.weight == null || data.weight.trim().isEmpty()) {
            return "- kg";
        }
        return data.weight.trim() + " kg";
    }

    private void runAssessment() {
        ArrayList<ExerciseData> allLogs = new ArrayList<>();
        for (ArrayList<ExerciseData> dayLogs : weeklyRoutine.values()) {
            allLogs.addAll(dayLogs);
        }
        if (allLogs.isEmpty()) {
            assessorFeedback.setText("Cannot evaluate yet.\n\nAdd exercises with sets, reps, weight, and rest time first.");
            return;
        }

        Map<String, Integer> setsByCategory = new HashMap<>();
        Map<String, Double> volumeByCategory = new HashMap<>();
        int totalSets = 0;
        double totalVolume = 0;
        int shortRestCount = 0;
        for (ExerciseData data : allLogs) {
            String category = classifyExercise(data.name);
            int sets = getTargetSets(data);
            double volume = estimateVolume(data);
            setsByCategory.put(category, setsByCategory.getOrDefault(category, 0) + sets);
            volumeByCategory.put(category, volumeByCategory.getOrDefault(category, 0.0) + volume);
            totalSets += sets;
            totalVolume += volume;
            if (data.restSeconds < 45) {
                shortRestCount++;
            }
        }

        int push = setsByCategory.getOrDefault("Push", 0);
        int pull = setsByCategory.getOrDefault("Pull", 0);
        int legs = setsByCategory.getOrDefault("Legs", 0);
        int categoriesCovered = setsByCategory.size();
        int score = Math.min(70, categoriesCovered * 14);
        if (push > 0 && pull > 0 && legs > 0) score += 15;
        if (totalSets >= 9 && totalSets <= 24) score += 10;
        if (shortRestCount == 0) score += 5;
        score = Math.min(100, score);

        StringBuilder report = new StringBuilder("SPLIT ASSESSMENT\n\n");
        report.append("Balance Score: ").append(score).append("%\n");
        report.append("Total planned sets: ").append(totalSets).append("\n");
        report.append("Estimated volume: ").append(String.format("%.0f", totalVolume)).append("\n\n");
        report.append("Coverage:\n");
        appendCategoryLine(report, "Push", setsByCategory, volumeByCategory);
        appendCategoryLine(report, "Pull", setsByCategory, volumeByCategory);
        appendCategoryLine(report, "Legs", setsByCategory, volumeByCategory);
        appendCategoryLine(report, "Arms", setsByCategory, volumeByCategory);
        appendCategoryLine(report, "Core", setsByCategory, volumeByCategory);

        report.append("\nCoach Notes:\n");
        if (push == 0) report.append("- Add a push movement such as a press or push-up.\n");
        if (pull == 0) report.append("- Add a pull movement such as a row or pulldown.\n");
        if (legs == 0) report.append("- Add a lower-body movement such as squat, lunge, or hinge.\n");
        if (push > 0 && pull > 0 && Math.abs(push - pull) > 4) {
            report.append("- Push and pull volume is uneven. Bring them within about 4 sets.\n");
        }
        if (totalSets < 9) report.append("- Overall set count is light. Add a few working sets for a fuller session.\n");
        if (totalSets > 24) report.append("- Volume is high. Watch fatigue and recovery.\n");
        if (shortRestCount > 0) report.append("- Some rest times are very short. Heavy sets usually need longer rest.\n");
        if (score >= 85) report.append("- Strong structure. Keep progressing load or reps gradually.\n");
        assessorFeedback.setText(report.toString());
    }

    private String classifyExercise(String name) {
        String ex = name == null ? "" : name.toLowerCase();
        if (ex.contains("bench") || ex.contains("press") || ex.contains("chest") || ex.contains("push") || ex.contains("shoulder")) return "Push";
        if (ex.contains("row") || ex.contains("pull") || ex.contains("lat") || ex.contains("deadlift") || ex.contains("back")) return "Pull";
        if (ex.contains("squat") || ex.contains("leg") || ex.contains("calf") || ex.contains("lunge") || ex.contains("ham") || ex.contains("glute")) return "Legs";
        if (ex.contains("curl") || ex.contains("tricep") || ex.contains("bicep") || ex.contains("extension")) return "Arms";
        if (ex.contains("plank") || ex.contains("crunch") || ex.contains("abs") || ex.contains("core")) return "Core";
        return "Other";
    }

    private void appendCategoryLine(StringBuilder report, String category, Map<String, Integer> sets, Map<String, Double> volume) {
        int setCount = sets.getOrDefault(category, 0);
        double categoryVolume = volume.getOrDefault(category, 0.0);
        report.append("- ").append(category).append(": ").append(setCount).append(" sets");
        if (categoryVolume > 0) {
            report.append(", ").append(String.format("%.0f", categoryVolume)).append(" volume");
        }
        report.append("\n");
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
                JPanel leftWrapper = new JPanel(new BorderLayout(8, 0));
                leftWrapper.setOpaque(false);
                JCheckBox completeBox = createHistoryCheckBox();
                completeBox.setSelected(isExerciseCompletedOnDate(data, currentSelectedDate));
                completeBox.setToolTipText("Add this exercise to History");
                completeBox.addActionListener(e -> setExerciseHistoryComplete(data, completeBox.isSelected()));
                JLabel titleLabel = new JLabel(data.name);
                titleLabel.setFont(Theme.BODY_BOLD);
                titleLabel.setForeground(Color.WHITE);
                leftWrapper.add(completeBox, BorderLayout.WEST);
                leftWrapper.add(titleLabel, BorderLayout.CENTER);
                exerciseCard.add(leftWrapper, BorderLayout.WEST);
                if (isEditMode) {
                    JButton deleteCardBtn = new RoundedButton("Delete", 8);
                    deleteCardBtn.setBackground(Theme.DANGER);
                    deleteCardBtn.setForeground(Color.WHITE);
                    deleteCardBtn.setPreferredSize(new Dimension(80, 28));
                    deleteCardBtn.addActionListener(e -> {
                        removeExerciseFromAllCompletedDates(data);
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

    private JCheckBox createHistoryCheckBox() {
        JCheckBox checkBox = new JCheckBox() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 6;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setPaint(Theme.verticalGradient(isSelected() ? Theme.ACCENT : Theme.PANEL_MID, size, size));
                g2.fillRoundRect(x, y, size, size, 8, 8);
                g2.setColor(isSelected() ? Theme.ACCENT_GLOW : Theme.BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, size, size, 8, 8);
                if (isSelected()) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 5, y + size / 2, x + size / 2 - 1, y + size - 6);
                    g2.drawLine(x + size / 2 - 1, y + size - 6, x + size - 5, y + 5);
                }
                g2.dispose();
            }
        };
        checkBox.setOpaque(false);
        checkBox.setContentAreaFilled(false);
        checkBox.setBorderPainted(false);
        checkBox.setFocusPainted(false);
        checkBox.setPreferredSize(new Dimension(26, 26));
        return checkBox;
    }

    private void setExerciseHistoryComplete(ExerciseData data, boolean completed) {
        ArrayList<ExerciseData> datedLogs = datedWorkoutLogs.computeIfAbsent(currentSelectedDate, day -> new ArrayList<>());
        if (completed) {
            if (!datedLogs.contains(data)) {
                datedLogs.add(data);
            }
        } else {
            datedLogs.remove(data);
        }
        if ("HISTORY".equals(activeScreenName)) {
            refreshExerciseChoices();
            updateHistoryChart();
        }
    }

    private boolean isExerciseCompletedOnDate(ExerciseData data, LocalDate date) {
        ArrayList<ExerciseData> datedLogs = datedWorkoutLogs.get(date);
        return datedLogs != null && datedLogs.contains(data);
    }

    private void removeExerciseFromAllCompletedDates(ExerciseData data) {
        for (ArrayList<ExerciseData> logs : datedWorkoutLogs.values()) {
            logs.remove(data);
        }
        for (Map<ExerciseData, Integer> daySets : loggedSetsByDate.values()) {
            daySets.remove(data);
        }
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
        if (historyChart == null) return;
        if (historyExerciseSelect.getSelectedItem() == null) {
            historyChart.setData(new ArrayList<>(), new ArrayList<>(), "Completed exercise");
            return;
        }
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
        if (weight <= 0) {
            return Math.max(1, sets) * Math.max(1, reps);
        }
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
        Object[] message = {
                "Date (YYYY-MM-DD):", dateField,
                "Bodyweight (kg):", weightField
        };
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

    private void setHeightDialog() {
        String input = JOptionPane.showInputDialog(this, "Height in cm:", heightCm > 0 ? heightCm : "");
        if (input != null) {
            try {
                heightCm = Double.parseDouble(input.trim());
                refreshBodyUI();
            } catch (NumberFormatException ignored) {
                JOptionPane.showMessageDialog(this, "Enter a valid height.");
            }
        }
    }

    private void refreshBodyUI() {
        bodyEntries.sort(Comparator.comparing(entry -> entry.date));
        bodyChart.setData(bodyEntries);
        bodyLogPanel.removeAll();
        if (heightSummaryButton != null) {
            heightSummaryButton.setText(heightCm > 0 ? String.format("%.0f cm", heightCm) : "Set height");
        }
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

    private void enableVerticalDragScroll(JScrollPane scrollPane) {
        MouseAdapter dragToScroll = new MouseAdapter() {
            private Point lastPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scrollPane.getViewport());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint == null) {
                    return;
                }
                Point currentPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scrollPane.getViewport());
                JViewport viewport = scrollPane.getViewport();
                Point viewPosition = viewport.getViewPosition();
                viewPosition.y += lastPoint.y - currentPoint.y;
                int maxY = Math.max(0, viewport.getView().getHeight() - viewport.getHeight());
                viewPosition.y = Math.max(0, Math.min(viewPosition.y, maxY));
                viewport.setViewPosition(viewPosition);
                lastPoint = currentPoint;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        };
        scrollPane.getViewport().addMouseListener(dragToScroll);
        scrollPane.getViewport().addMouseMotionListener(dragToScroll);
        scrollPane.getViewport().getView().addMouseListener(dragToScroll);
        scrollPane.getViewport().getView().addMouseMotionListener(dragToScroll);
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

            g2.setColor(new Color(255, 255, 255, 7));
            for (int x = 0; x < getWidth(); x += 42) {
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += 42) {
                g2.drawLine(0, y, getWidth(), y);
            }

            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(new Color(255, 255, 255, 13));
            for (int y = 96; y < getHeight() + 120; y += 78) {
                g2.drawLine(-40, y, getWidth() + 40, y - 48);
            }

            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 26));
            int[] bandX = {-80, getWidth() / 2, getWidth() + 80, getWidth() / 2};
            int[] bandY = {170, 118, 430, 482};
            g2.fillPolygon(bandX, bandY, 4);

            g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), 18));
            int[] band2X = {getWidth() - 120, getWidth() + 40, getWidth() + 40, getWidth() - 210};
            int[] band2Y = {30, 18, 310, 360};
            g2.fillPolygon(band2X, band2Y, 4);

            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
