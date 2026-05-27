import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GymApp extends JFrame {

    private final ArrayList<ExerciseData>                        masterExercises  = new ArrayList<>();
    private final Map<LocalDate, ArrayList<ExerciseData>>        datedWorkoutLogs = new HashMap<>();
    private final Map<LocalDate, Map<ExerciseData, Integer>>     loggedSetsByDate = new HashMap<>();
    private final ArrayList<LocalDate>                           timelineDates    = new ArrayList<>();
    private final ArrayList<BodyWeightEntry>                     bodyEntries      = new ArrayList<>();
    private final ArrayList<RecordData>                          personalRecords  = new ArrayList<>();
    private final ArrayList<CardioEntry>                         cardioEntries    = new ArrayList<>();
    private final Set<DayOfWeek>                                 restDays         = new HashSet<>();

    private final Map<String, Screen> screens = new HashMap<>();
    private CardLayout cardLayout;
    private JPanel     containerPanel;
    private String     activeScreenName = "WORKOUT";

    private JPanel  openMenuPanel;
    private JPanel  openMenuWrapper;
    private JButton openMenuButton;

    private LocalDate    currentSelectedDate;
    private JPanel       exerciseListPanel;
    private JButton      globalEditBtn;
    private boolean      isEditMode;
    private RoundedPanel selectedDayBox;
    private Point        dragStartPoint;
    private StreakPanel  streakPanel;
    private JPanel       calendarPanel;

    private static final String[] MACRO_LABELS         = {"Calories", "Protein (g)", "Carbs (g)", "Fats (g)"};
    private static final String[] MACRO_INTAKE_LABELS  = {"Calories eaten", "Protein eaten (g)", "Carbs eaten (g)", "Fats eaten (g)"};
    private final String[] macroGoalValues    = {"", "", "", ""};
    private final String[] macroCurrentValues = {"", "", "", ""};
    private boolean isMacroEditMode;
    private JPanel  macroCardsContainer;
    private JButton macroEditBtn;

    private boolean isPrEditMode;
    private JPanel  prCardsContainer;
    private JButton prEditBtn;

    private JComboBox<String> historyExerciseSelect;
    private BarChartPanel     historyChart;

    private JPanel         bodyLogPanel;
    private LineChartPanel bodyChart;
    private JButton        heightSummaryButton;
    private double         heightCm;

    private JPanel    cardioLogPanel;
    private JTextArea assessorFeedback;

    private ExerciseData      selectedExercise;
    private JLabel            exerciseDetailTitle;
    private JLabel            exerciseDetailStats;
    private JLabel            restTimerLabel;
    private javax.swing.Timer restTimer;
    private int               remainingRestSeconds;

    private static final String[][] TEMPLATES = {
        {"PPL \u2013 Push Day",  "Bench Press|80|4x8|120","Overhead Press|50|4x8|120","Incline DB Press|30|3x10|90","Lateral Raise|12|3x15|60","Tricep Pushdown|25|3x12|60"},
        {"PPL \u2013 Pull Day",  "Deadlift|100|4x6|180","Barbell Row|70|4x8|120","Lat Pulldown|60|3x10|90","Face Pull|20|3x15|60","Bicep Curl|15|3x12|60"},
        {"PPL \u2013 Leg Day",   "Squat|90|4x8|180","Romanian Deadlift|70|3x10|120","Leg Press|120|3x12|90","Leg Curl|40|3x12|60","Calf Raise|60|4x15|45"},
        {"Upper Body",           "Bench Press|80|4x8|120","Barbell Row|70|4x8|120","Overhead Press|50|3x10|90","Lat Pulldown|60|3x10|90","Bicep Curl|15|3x12|60","Tricep Extension|20|3x12|60"},
        {"Lower Body",           "Squat|90|4x8|180","Romanian Deadlift|70|3x10|120","Leg Press|120|3x12|90","Leg Curl|40|3x12|60","Calf Raise|60|4x15|45"},
        {"Full Body",            "Squat|80|3x8|120","Bench Press|70|3x8|120","Barbell Row|60|3x8|120","Overhead Press|40|3x10|90","Romanian Deadlift|60|3x10|120"},
    };

    public GymApp() {
        Theme.setDark(AppSettings.isDarkMode());
        heightCm = AppSettings.getHeightCm();

        setTitle("IronPulse Gym Tracker");
        setIconImage(createAppIcon());
        setSize(480, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout     = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        setContentPane(containerPanel);

        installMenuDismissListener();
        initTimelineAndDate();
        loadData();

        addScreen("WORKOUT",    createMainWorkoutScreen());
        addScreen("MACRO",      createMacroTrackerScreen());
        addScreen("PR",         createPersonalRecordsScreen());
        addScreen("HISTORY",    createHistoryScreen());
        addScreen("BODY",       createBodyScreen());
        addScreen("CARDIO",     createCardioScreen());
        addScreen("ASSESSMENT", createAssessmentScreen());
        addScreen("EXERCISE",   createExerciseDetailScreen());
        addScreen("SETTINGS",   createSettingsScreen());

        navigateTo("WORKOUT");
    }

    private ArrayList<ExerciseData> getExercisesForDate(LocalDate date) {
        // An exercise belongs to 'date' if:
        //  - it was added on or before 'date'
        //  - 'date' is on or after addedDate and the number of days between them is a multiple of 7
        //  - it is not marked deleted after a specific date (handled by addedDate being per-occurrence)
        ArrayList<ExerciseData> result = new ArrayList<>();
        for (ExerciseData ex : masterExercises) {
            LocalDate added = ex.getAddedDate();
            if (added.isAfter(date)) continue;
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(added, date);
            if (daysBetween % 7 == 0) result.add(ex);
        }
        return result;
    }

    private boolean dateHasExercises(LocalDate date) {
        for (ExerciseData ex : masterExercises) {
            LocalDate added = ex.getAddedDate();
            if (added.isAfter(date)) continue;
            long diff = java.time.temporal.ChronoUnit.DAYS.between(added, date);
            if (diff % 7 == 0) return true;
        }
        return false;
    }

    private void loadData() {
        masterExercises.addAll(DataStore.loadExercises());

        Map<LocalDate, Set<String[]>> completedEntries = DataStore.loadCompletedEntries();
        for (Map.Entry<LocalDate, Set<String[]>> e : completedEntries.entrySet()) {
            LocalDate date = e.getKey();
            ArrayList<ExerciseData> dayEx = getExercisesForDate(date);
            ArrayList<ExerciseData> completed = new ArrayList<>();
            for (String[] pair : e.getValue()) {
                String name = pair[0];
                LocalDate addedDate = LocalDate.parse(pair[1]);
                dayEx.stream()
                     .filter(ex -> ex.getName().equals(name) && ex.getAddedDate().equals(addedDate))
                     .findFirst()
                     .ifPresent(completed::add);
            }
            if (!completed.isEmpty()) datedWorkoutLogs.put(date, completed);
        }

        ArrayList<RecordData> loaded = DataStore.loadRecords();
        if (loaded.isEmpty()) {
            personalRecords.add(new RecordData("Squat",        ""));
            personalRecords.add(new RecordData("Bench Press",  ""));
            personalRecords.add(new RecordData("Deadlift",     ""));
            personalRecords.add(new RecordData("Overhead Press",""));
            personalRecords.add(new RecordData("Barbell Row",  ""));
        } else {
            personalRecords.addAll(loaded);
        }
        bodyEntries.addAll(DataStore.loadBody());
        cardioEntries.addAll(DataStore.loadCardio());
        restDays.addAll(DataStore.loadRestDays());
        String[][] macros = DataStore.loadMacros();
        System.arraycopy(macros[0], 0, macroGoalValues,    0, 4);
        System.arraycopy(macros[1], 0, macroCurrentValues, 0, 4);
    }

    private void save() {
        DataStore.saveAll(masterExercises, datedWorkoutLogs, personalRecords,
                bodyEntries, macroGoalValues, macroCurrentValues, cardioEntries, restDays);
    }

    private void initTimelineAndDate() {
        LocalDate today = LocalDate.now();
        currentSelectedDate = today;
        for (int i = -13; i <= 14; i++) timelineDates.add(today.plusDays(i));
    }

    private Image createAppIcon() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), size, size));
        g2.fillRoundRect(3,3,size-6,size-6,18,18);
        g2.setColor(Theme.ACCENT_GLOW); g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(4,4,size-9,size-9,18,18);
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT,size,size));
        g2.fillRoundRect(15,29,34,7,7,7); g2.fillRoundRect(9,22,10,21,7,7); g2.fillRoundRect(45,22,10,21,7,7);
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.drawLine(25,45,31,18); g2.drawLine(31,18,39,36); g2.drawLine(39,36,46,17);
        g2.dispose();
        return img;
    }

    private void addScreen(String name, JPanel screen) {
        containerPanel.add(screen, name);
        if (screen instanceof Screen) screens.put(name, (Screen) screen);
    }

    private void navigateTo(String name) {
        closeOpenMenu();
        activeScreenName = name;
        cardLayout.show(containerPanel, name);
        Screen s = screens.get(name);
        if (s != null) s.onNavigateTo();
    }

    private AppScreen createScreenShell(String title, String iconType) {
        AppScreen screen = new AppScreen(new BorderLayout());
        screen.setOpaque(false);
        screen.setBackground(Theme.BG_DARK());
        screen.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        IconHeader header = new IconHeader(title, iconType);
        header.setOnSettingsClick(() -> navigateTo("SETTINGS"));
        screen.add(header, BorderLayout.NORTH);
        screen.add(createBottomActionBar(title), BorderLayout.SOUTH);
        return screen;
    }

    private JPanel createBottomActionBar(String activeTitle) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel menu = new RoundedPanel(22, Theme.PANEL_DARK());
        menu.setLayout(new GridLayout(7, 1, 0, 4));
        menu.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        menu.setMaximumSize(new Dimension(430, 272));
        menu.setPreferredSize(new Dimension(430, 272));
        menu.setVisible(false);

        menu.add(navMenuButton("WORKOUT",    "Lift",       "Workout".equals(activeTitle),    "workout"));
        menu.add(navMenuButton("MACRO",      "Macros",     "Macros".equals(activeTitle),     "macro"));
        menu.add(navMenuButton("PR",         "1RM / PRs",  "1RM / PRs".equals(activeTitle),  "pr"));
        menu.add(navMenuButton("HISTORY",    "History",    "History".equals(activeTitle),    "history"));
        menu.add(navMenuButton("BODY",       "Body",       "Body".equals(activeTitle),       "body"));
        menu.add(navMenuButton("CARDIO",     "Cardio",     "Cardio".equals(activeTitle),     "cardio"));
        menu.add(navMenuButton("ASSESSMENT", "Assessment", "Assessment".equals(activeTitle), "assessment"));

        JPanel circles = new JPanel(new BorderLayout());
        circles.setOpaque(false);
        circles.setMaximumSize(new Dimension(430, 66));
        circles.setPreferredSize(new Dimension(430, 66));

        JButton menuButton = circleButton("menu");
        menuButton.addActionListener(e -> toggleMenu(menu, menuButton, wrapper));
        circles.add(menuButton, BorderLayout.WEST);

        if (screenHasPlusAction(activeTitle)) {
            JButton addButton = circleButton("plus");
            addButton.addActionListener(e -> handlePlusAction());
            circles.add(addButton, BorderLayout.EAST);
        }

        wrapper.add(menu);
        wrapper.add(Box.createRigidArea(new Dimension(0, 8)));
        wrapper.add(circles);
        return wrapper;
    }

    private void toggleMenu(JPanel menu, JButton menuButton, JPanel wrapper) {
        if (openMenuPanel != null && openMenuPanel != menu) closeOpenMenu();
        boolean nowVisible = !menu.isVisible();
        menu.setVisible(nowVisible);
        openMenuPanel   = nowVisible ? menu      : null;
        openMenuWrapper = nowVisible ? wrapper    : null;
        openMenuButton  = nowVisible ? menuButton : null;
        wrapper.revalidate(); wrapper.repaint();
    }

    private void closeOpenMenu() {
        if (openMenuPanel != null) openMenuPanel.setVisible(false);
        if (openMenuWrapper != null) { openMenuWrapper.revalidate(); openMenuWrapper.repaint(); }
        openMenuPanel = null; openMenuWrapper = null; openMenuButton = null;
    }

    private void installMenuDismissListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            if (me.getID() != MouseEvent.MOUSE_PRESSED) return;
            if (openMenuPanel == null || !openMenuPanel.isVisible()) return;
            Component src = me.getComponent();
            boolean inMenu   = src != null && SwingUtilities.isDescendingFrom(src, openMenuPanel);
            boolean inButton = src != null && openMenuButton != null && SwingUtilities.isDescendingFrom(src, openMenuButton);
            if (!inMenu && !inButton) closeOpenMenu();
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private boolean screenHasPlusAction(String t) {
        return "Workout".equals(t) || "Body".equals(t) || "1RM / PRs".equals(t) || "Cardio".equals(t);
    }

    private void handlePlusAction() {
        switch (activeScreenName) {
            case "PR":     addRecordDialog();    break;
            case "BODY":   addBodyEntryDialog(); break;
            case "CARDIO": addCardioDialog();    break;
            default:       addExerciseDialog();  break;
        }
    }

    private JButton navMenuButton(String target, String label, boolean active, String iconType) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(Theme.CARD_BG_SELECTED());
                    g2.fillRoundRect(2,2,getWidth()-4,getHeight()-4,16,16);
                    g2.setColor(Theme.ACCENT_GLOW); g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(2,2,getWidth()-5,getHeight()-5,16,16);
                }
                g2.setColor(active ? Theme.ACCENT : Theme.TEXT_MUTED());
                drawMiniIcon(g2, iconType, 14, getHeight()/2-10);
                g2.setFont(Theme.SMALL_BOLD); g2.setColor(active ? Theme.ACCENT : Theme.TEXT_MUTED());
                g2.drawString(label, 46, getHeight()/2+5);
                g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.addActionListener(e -> navigateTo(target));
        return btn;
    }

    private JButton circleButton(String type) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d=Math.min(getWidth(),getHeight())-8, bx=(getWidth()-d)/2, by=(getHeight()-d)/2;
                g2.setPaint(Theme.verticalGradient(Theme.ACCENT,getWidth(),getHeight()));
                g2.fillOval(bx,by,d,d);
                g2.setColor(Theme.ACCENT_GLOW); g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(bx-1,by-1,d+1,d+1);
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int cx=getWidth()/2, cy=getHeight()/2;
                if ("plus".equals(type)) { g2.drawLine(cx-11,cy,cx+11,cy); g2.drawLine(cx,cy-11,cx,cy+11); }
                else { g2.drawLine(cx-12,cy-8,cx+12,cy-8); g2.drawLine(cx-12,cy,cx+12,cy); g2.drawLine(cx-12,cy+8,cx+12,cy+8); }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(58,58)); btn.setMinimumSize(new Dimension(58,58));
        btn.setMaximumSize(new Dimension(58,58)); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        return btn;
    }

    private void drawMiniIcon(Graphics2D g2, String type, int x, int y) {
        g2.setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK,22,22));
        g2.fillRoundRect(x-2,y-2,24,24,9,9);
        g2.setColor(new Color(255,255,255,210));
        switch (type) {
            case "workout":    g2.drawLine(x,y+10,x+20,y+10); g2.drawLine(x+3,y+5,x+3,y+15); g2.drawLine(x+17,y+5,x+17,y+15); break;
            case "macro":      g2.drawOval(x+2,y+2,16,16); g2.drawLine(x+10,y+9,x+17,y+3); break;
            case "pr":         g2.drawLine(x+10,y+2,x+10,y+17); g2.drawLine(x+4,y+8,x+10,y+2); g2.drawLine(x+16,y+8,x+10,y+2); break;
            case "history":    g2.fillRoundRect(x+2,y+11,4,7,3,3); g2.fillRoundRect(x+8,y+7,4,11,3,3); g2.fillRoundRect(x+14,y+3,4,15,3,3); break;
            case "cardio":     g2.drawOval(x+2,y+2,16,16); g2.drawLine(x+10,y+10,x+15,y+6); g2.drawLine(x+3,y+19,x+17,y+19); break;
            case "assessment": g2.drawOval(x+2,y+2,16,16); g2.drawLine(x+10,y+5,x+10,y+11); g2.drawLine(x+10,y+11,x+15,y+8); break;
            default:           g2.drawLine(x+1,y+18,x+19,y+18); g2.drawLine(x+1,y+18,x+1,y+2); g2.drawLine(x+1,y+14,x+8,y+10); g2.drawLine(x+8,y+10,x+13,y+12); g2.drawLine(x+13,y+12,x+19,y+4); break;
        }
    }

    private JPanel createMainWorkoutScreen() {
        AppScreen screen = new AppScreen(new BorderLayout());
        screen.setOpaque(false); screen.setBackground(Theme.BG_DARK());
        screen.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));

        JPanel fixedTop = new JPanel();
        fixedTop.setOpaque(false);
        fixedTop.setLayout(new BoxLayout(fixedTop, BoxLayout.Y_AXIS));
        IconHeader workoutHeader = new IconHeader("Workout", "workout");
        workoutHeader.setOnSettingsClick(() -> navigateTo("SETTINGS"));
        fixedTop.add(workoutHeader);
        fixedTop.add(Box.createRigidArea(new Dimension(0, 8)));
        fixedTop.add(createTimelinePanel());
        fixedTop.add(Box.createRigidArea(new Dimension(0, 8)));
        streakPanel = new StreakPanel();
        fixedTop.add(streakPanel);
        fixedTop.add(Box.createRigidArea(new Dimension(0, 10)));
        screen.add(fixedTop, BorderLayout.NORTH);
        screen.add(createBottomActionBar("Workout"), BorderLayout.SOUTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(440, 36));
        headerRow.setPreferredSize(new Dimension(440, 36));
        JLabel logHeader = new JLabel("Today's Targets:");
        logHeader.setForeground(Theme.TEXT()); logHeader.setFont(Theme.HEADER_FONT);
        headerRow.add(logHeader, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);
        RoundedButton templateBtn = new RoundedButton("Template", 10);
        templateBtn.setBackground(Theme.PANEL_MID()); templateBtn.setForeground(Theme.TEXT());
        templateBtn.setPreferredSize(new Dimension(92, 30));
        templateBtn.addActionListener(e -> showTemplateDialog());
        btnRow.add(templateBtn);
        globalEditBtn = new RoundedButton("Edit", 10);
        globalEditBtn.setBackground(Theme.ACCENT); globalEditBtn.setForeground(Color.WHITE);
        globalEditBtn.setPreferredSize(new Dimension(70, 30));
        globalEditBtn.addActionListener(e -> {
            isEditMode = !isEditMode;
            globalEditBtn.setText(isEditMode ? "Done" : "Edit");
            globalEditBtn.setBackground(isEditMode ? Theme.DANGER : Theme.ACCENT);
            refreshExerciseListUI();
        });
        btnRow.add(globalEditBtn);
        headerRow.add(btnRow, BorderLayout.EAST);

        content.add(headerRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(createLiftColumnHeader());
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        exerciseListPanel = new JPanel();
        exerciseListPanel.setLayout(new BoxLayout(exerciseListPanel, BoxLayout.Y_AXIS));
        exerciseListPanel.setOpaque(false);
        JScrollPane listScroll = new JScrollPane(exerciseListPanel);
        listScroll.setPreferredSize(new Dimension(420, 180));
        styleScroll(listScroll); enableVerticalDragScroll(listScroll);
        content.add(listScroll);

        screen.add(content, BorderLayout.CENTER);
        screen.setNavigateHook(this::refreshWorkoutScreen);
        return screen;
    }

    private void refreshWorkoutScreen() {
        refreshExerciseListUI();
        refreshAllDayBoxColours();
        if (streakPanel != null) streakPanel.update(masterExercises, datedWorkoutLogs, restDays);
    }

    private void showTemplateDialog() {
        String[] names = Arrays.stream(TEMPLATES).map(t -> t[0]).toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(this, "Choose a template to load:",
                "Workout Templates", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (choice == null) return;
        for (String[] template : TEMPLATES) {
            if (!template[0].equals(choice)) continue;
            for (int i = 1; i < template.length; i++) {
                String[] p = template[i].split("\\|");
                masterExercises.add(new ExerciseData(p[0], p[1], p[2],
                        Integer.parseInt(p[3]), currentSelectedDate));
            }
            refreshWorkoutScreen(); save();
            break;
        }
    }

    private JComponent createTimelinePanel() {
        calendarPanel = new JPanel();
        calendarPanel.setLayout(new BoxLayout(calendarPanel, BoxLayout.X_AXIS));
        calendarPanel.setOpaque(false);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");
        DateTimeFormatter numFmt = DateTimeFormatter.ofPattern("dd");

        for (LocalDate date : timelineDates) {
            RoundedPanel dayBox = buildDayBox(date, dayFmt, numFmt);
            calendarPanel.add(dayBox);
            calendarPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        }

        JScrollPane scroll = new JScrollPane(calendarPanel);
        scroll.setPreferredSize(new Dimension(420, 92));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        styleScroll(scroll);
        installHorizontalDragScroll(calendarPanel, scroll);

        SwingUtilities.invokeLater(() -> {
            if (selectedDayBox != null) {
                int x = Math.max(0, selectedDayBox.getX() - 16);
                scroll.getViewport().setViewPosition(new Point(x, 0));
            }
        });
        return scroll;
    }

    private RoundedPanel buildDayBox(LocalDate date, DateTimeFormatter dayFmt, DateTimeFormatter numFmt) {
        RoundedPanel dayBox = new RoundedPanel(14, Theme.PANEL_DARK());
        dayBox.putClientProperty("date", date);
        dayBox.setLayout(new BoxLayout(dayBox, BoxLayout.Y_AXIS));
        dayBox.setPreferredSize(new Dimension(62, 80));
        dayBox.setMaximumSize(new Dimension(62, 80));
        dayBox.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel dayLabel = transparentLabel(date.format(dayFmt).toUpperCase(), SwingConstants.CENTER, Theme.TINY, Theme.TEXT_DIM());
        dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel numLabel = transparentLabel(date.format(numFmt), SwingConstants.CENTER, Theme.HEADER_FONT, Theme.TEXT());
        numLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Colour dot: shows workout / rest / empty status
        // Drawn directly as a JLabel subclass so one repaint() on dayBox refreshes everything
        JLabel dot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isRest  = restDays.contains(date.getDayOfWeek());
                boolean hasWork = dateHasExercises(date);
                boolean isToday = date.equals(LocalDate.now());
                Color col;
                if (isToday)       col = Theme.CAL_TODAY();
                else if (isRest)   col = Theme.CAL_REST();
                else if (hasWork)  col = Theme.CAL_WORKOUT();
                else               col = Theme.TEXT_DIM();
                g2.setColor(col);
                int d = 7;
                g2.fillOval((getWidth()-d)/2, (getHeight()-d)/2, d, d);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 12));
        dot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        dot.setAlignmentX(Component.CENTER_ALIGNMENT);

        dayBox.add(Box.createRigidArea(new Dimension(0, 2)));
        dayBox.add(dayLabel);
        dayBox.add(Box.createRigidArea(new Dimension(0, 4)));
        dayBox.add(numLabel);
        dayBox.add(Box.createRigidArea(new Dimension(0, 4)));
        dayBox.add(dot);

        applyDateBoxState(dayBox, date, date.equals(currentSelectedDate));
        if (date.equals(currentSelectedDate)) selectedDayBox = dayBox;

        dayBox.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (selectedDayBox != null)
                    applyDateBoxState(selectedDayBox, (LocalDate) selectedDayBox.getClientProperty("date"), false);
                selectedDayBox = dayBox; currentSelectedDate = date;
                applyDateBoxState(dayBox, date, true);
                refreshWorkoutScreen();
            }
        });
        return dayBox;
    }

    private JLabel transparentLabel(String text, int align, Font font, Color fg) {
        JLabel lbl = new JLabel(text, align);
        lbl.setFont(font); lbl.setForeground(fg); lbl.setOpaque(false);
        return lbl;
    }

    private void applyDateBoxState(RoundedPanel dayBox, LocalDate date, boolean selected) {
        LocalDate today = LocalDate.now();
        boolean isToday   = date.equals(today);
        boolean isPast    = date.isBefore(today);
        boolean isRest    = restDays.contains(date.getDayOfWeek());
        boolean hasWork   = dateHasExercises(date);

        if (selected) {
            // Bright selection ring regardless of day type
            dayBox.setBackgroundColor(Theme.CARD_BG_SELECTED());
            dayBox.setSelectedGlow(true);
        } else if (isToday) {
            dayBox.setBackgroundColor(Theme.CAL_TODAY());
            dayBox.setSelectedGlow(true);
        } else if (isPast) {
            // Past days: just show muted, don't mislead with colours
            dayBox.setBackgroundColor(Theme.PANEL_DARK());
            dayBox.setSelectedGlow(false);
        } else if (isRest) {
            dayBox.setBackgroundColor(Theme.CAL_REST());
            dayBox.setSelectedGlow(false);
        } else if (hasWork) {
            dayBox.setBackgroundColor(Theme.CAL_WORKOUT());
            dayBox.setSelectedGlow(false);
        } else {
            dayBox.setBackgroundColor(Theme.CAL_EMPTY());
            dayBox.setSelectedGlow(false);
        }
    }

    private void refreshAllDayBoxColours() {
        if (calendarPanel == null) return;
        for (Component c : calendarPanel.getComponents()) {
            if (!(c instanceof RoundedPanel)) continue;
            RoundedPanel dayBox = (RoundedPanel) c;
            Object prop = dayBox.getClientProperty("date");
            if (!(prop instanceof LocalDate)) continue;
            LocalDate date = (LocalDate) prop;
            applyDateBoxState(dayBox, date, date.equals(currentSelectedDate));
            // repaint cascades to all lightweight children including the dot JLabel
            dayBox.repaint();
        }
        calendarPanel.repaint();
    }

    private JPanel createLiftColumnHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false); header.setMaximumSize(new Dimension(420, 20));
        JLabel left  = transparentLabel("Exercise", SwingConstants.LEFT, Theme.SMALL_BOLD, Theme.TEXT_MUTED());
        JLabel right = transparentLabel("Weight | Sets x Reps | Rest", SwingConstants.RIGHT, Theme.SMALL_BOLD, Theme.TEXT_MUTED());
        header.add(left, BorderLayout.WEST); header.add(right, BorderLayout.EAST);
        return header;
    }

    private void refreshExerciseListUI() {
        if (exerciseListPanel == null) return;
        exerciseListPanel.removeAll();
        ArrayList<ExerciseData> currentLogs = getExercisesForDate(currentSelectedDate);
        boolean isRestDay = restDays.contains(currentSelectedDate.getDayOfWeek());
        if (currentLogs.isEmpty()) {
            if (isRestDay) {
                exerciseListPanel.add(buildRestDayCard());
            } else {
                exerciseListPanel.add(createEmptyStateCard("No exercises yet", "Tap + to add or use a Template."));
                exerciseListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                exerciseListPanel.add(buildRestDayToggleCard(false));
            }
        } else {
            for (ExerciseData data : currentLogs) {
                exerciseListPanel.add(buildExerciseCard(currentLogs, data));
                exerciseListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        exerciseListPanel.revalidate(); exerciseListPanel.repaint();
    }

    private JPanel buildRestDayCard() {
        RoundedPanel card = new RoundedPanel(15, new Color(50, 40, 70));
        card.setLayout(new BorderLayout(10, 0));
        card.setMaximumSize(new Dimension(420, 58)); card.setPreferredSize(new Dimension(420, 58));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS)); left.setOpaque(false);
        left.add(transparentLabel("\uD83D\uDECC  Rest Day", SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()));
        left.add(transparentLabel("All " + currentSelectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "s are rest days", SwingConstants.LEFT, Theme.SMALL, Theme.TEXT_MUTED()));
        card.add(left, BorderLayout.WEST);
        RoundedButton removeBtn = new RoundedButton("Remove", 8);
        removeBtn.setBackground(Theme.PANEL_MID()); removeBtn.setForeground(Theme.TEXT());
        removeBtn.setPreferredSize(new Dimension(72, 28));
        removeBtn.addActionListener(e -> {
            restDays.remove(currentSelectedDate.getDayOfWeek());
            refreshWorkoutScreen(); save();
        });
        card.add(removeBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel buildRestDayToggleCard(boolean active) {
        RoundedPanel card = new RoundedPanel(15, Theme.PANEL_DARK());
        card.setLayout(new BorderLayout(10, 0));
        card.setMaximumSize(new Dimension(420, 52)); card.setPreferredSize(new Dimension(420, 52));
        card.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        card.add(transparentLabel("Mark as Rest Day", SwingConstants.LEFT, Theme.BODY, Theme.TEXT_MUTED()), BorderLayout.WEST);
        RoundedButton btn = new RoundedButton("Set Rest Day", 8);
        btn.setBackground(new Color(80, 60, 110)); btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(100, 28));
        btn.addActionListener(e -> {
            restDays.add(currentSelectedDate.getDayOfWeek());
            refreshWorkoutScreen(); save();
        });
        card.add(btn, BorderLayout.EAST);
        return card;
    }

    private JPanel buildExerciseCard(ArrayList<ExerciseData> logs, ExerciseData data) {
        RoundedPanel card = new RoundedPanel(15, Theme.CARD_BG());
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(420, 55)); card.setPreferredSize(new Dimension(420, 55));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel leftWrapper = new JPanel(new BorderLayout(8, 0));
        leftWrapper.setOpaque(false);
        JCheckBox completeBox = createHistoryCheckBox();
        completeBox.setSelected(isExerciseCompletedOnDate(data, currentSelectedDate));
        completeBox.addActionListener(e -> {
            setExerciseHistoryComplete(data, completeBox.isSelected());
            if (streakPanel != null) streakPanel.update(masterExercises, datedWorkoutLogs, restDays);
            save();
        });
        JLabel titleLabel = transparentLabel(data.getName(), SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT());
        leftWrapper.add(completeBox, BorderLayout.WEST);
        leftWrapper.add(titleLabel, BorderLayout.CENTER);
        card.add(leftWrapper, BorderLayout.WEST);

        if (isEditMode) {
            JPanel editBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            editBtns.setOpaque(false);
            RoundedButton editBtn = new RoundedButton("Edit", 8);
            editBtn.setBackground(Theme.ACCENT_DARK); editBtn.setForeground(Color.WHITE);
            editBtn.setPreferredSize(new Dimension(50, 28));
            editBtn.addActionListener(e -> editExerciseDialog(data));
            editBtns.add(editBtn);
            RoundedButton deleteBtn = new RoundedButton("Del", 8);
            deleteBtn.setBackground(Theme.DANGER); deleteBtn.setForeground(Color.WHITE);
            deleteBtn.setPreferredSize(new Dimension(44, 28));
            deleteBtn.addActionListener(e -> {
                removeExerciseFromAllCompletedDates(data);
                masterExercises.remove(data); refreshWorkoutScreen(); save();
            });
            editBtns.add(deleteBtn);
            card.add(editBtns, BorderLayout.EAST);
        } else {
            JLabel statsLabel = transparentLabel(formatExerciseSummaryCompact(data), SwingConstants.RIGHT, Theme.SMALL, Theme.TEXT_MUTED());
            card.add(statsLabel, BorderLayout.EAST);
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedExercise = data; navigateTo("EXERCISE");
                }
            });
        }
        return card;
    }

    private JPanel createMacroTrackerScreen() {
        AppScreen panel = createScreenShell("Macros", "macro");
        JPanel center = new JPanel(new BorderLayout(0, 8)); center.setOpaque(false);

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); topRow.setOpaque(false);
        RoundedButton presetBtn = new RoundedButton("Use Preset", 10);
        presetBtn.setBackground(Theme.ACCENT_DARK); presetBtn.setForeground(Color.WHITE);
        presetBtn.setPreferredSize(new Dimension(110, 30));
        presetBtn.addActionListener(e -> showMacroPresetDialog());
        topRow.add(presetBtn);
        macroEditBtn = new RoundedButton("Edit Stats", 10);
        macroEditBtn.setPreferredSize(new Dimension(100, 30));
        macroEditBtn.addActionListener(e -> {
            isMacroEditMode = !isMacroEditMode;
            updateEditButton(macroEditBtn, isMacroEditMode);
            refreshMacroCards(); if (!isMacroEditMode) save();
        });
        topRow.add(macroEditBtn);
        center.add(topRow, BorderLayout.NORTH);

        macroCardsContainer = new JPanel();
        macroCardsContainer.setLayout(new BoxLayout(macroCardsContainer, BoxLayout.Y_AXIS));
        macroCardsContainer.setOpaque(false);
        JScrollPane scroll = new JScrollPane(macroCardsContainer);
        styleScroll(scroll); enableVerticalDragScroll(scroll);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        panel.setNavigateHook(this::refreshMacroCards);
        refreshMacroCards();
        return panel;
    }

    private void showMacroPresetDialog() {
        String[] goals   = {"Weight Loss", "Muscle Building", "Maintenance", "Lean Bulk", "Cut (aggressive)"};
        String[] sexes   = {"Male", "Female"};
        JComboBox<String> goalBox = new JComboBox<>(goals);
        JComboBox<String> sexBox  = new JComboBox<>(sexes);
        JTextField weightKgField  = new JTextField();
        JPanel p = new JPanel(new GridLayout(3,2,10,10)); p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Goal:"));                          p.add(goalBox);
        p.add(new JLabel("Sex:"));                           p.add(sexBox);
        p.add(new JLabel("Current bodyweight (kg):"));       p.add(weightKgField);
        if (JOptionPane.showConfirmDialog(this, p, "Macro Preset Calculator", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        double bw;
        try { bw = Double.parseDouble(weightKgField.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter a valid bodyweight in kg."); return; }
        boolean male   = "Male".equals(sexBox.getSelectedItem());
        String  goal   = (String) goalBox.getSelectedItem();

        // Protein: 1.8–2.2 g/kg, Carbs and Fats vary by goal, Calories = P*4 + C*4 + F*9
        double protein, carbs, fats, calories;
        switch (goal) {
            case "Weight Loss":
                calories = bw * (male ? 28 : 26);
                protein  = bw * 2.0;
                fats     = bw * 0.8;
                carbs    = (calories - protein * 4 - fats * 9) / 4;
                break;
            case "Muscle Building":
                calories = bw * (male ? 38 : 34);
                protein  = bw * 2.2;
                fats     = bw * 1.0;
                carbs    = (calories - protein * 4 - fats * 9) / 4;
                break;
            case "Lean Bulk":
                calories = bw * (male ? 35 : 32);
                protein  = bw * 2.0;
                fats     = bw * 0.9;
                carbs    = (calories - protein * 4 - fats * 9) / 4;
                break;
            case "Cut (aggressive)":
                calories = bw * (male ? 24 : 22);
                protein  = bw * 2.4;
                fats     = bw * 0.7;
                carbs    = (calories - protein * 4 - fats * 9) / 4;
                break;
            default: // Maintenance
                calories = bw * (male ? 33 : 30);
                protein  = bw * 1.8;
                fats     = bw * 1.0;
                carbs    = (calories - protein * 4 - fats * 9) / 4;
                break;
        }
        carbs = Math.max(50, carbs); // floor

        macroGoalValues[0] = String.format("%.0f kcal", calories);
        macroGoalValues[1] = String.format("%.0f g", protein);
        macroGoalValues[2] = String.format("%.0f g", Math.round(carbs));
        macroGoalValues[3] = String.format("%.0f g", fats);

        JOptionPane.showMessageDialog(this, String.format(
                "Preset applied for %s — %s\n\nCalories: %.0f kcal\nProtein: %.0f g\nCarbs: %.0f g\nFats: %.0f g\n\n" +
                "These are starting estimates. Adjust based on progress.",
                goal, male ? "male" : "female", calories, protein, carbs, fats));

        isMacroEditMode = false;
        refreshMacroCards();
        save();
    }

    private void refreshMacroCards() {
        macroCardsContainer.removeAll();
        macroCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));

        addSectionLabel(macroCardsContainer, "Daily Targets (Goal)");
        addStatCards(macroCardsContainer, MACRO_LABELS, macroGoalValues, isMacroEditMode);
        macroCardsContainer.add(Box.createRigidArea(new Dimension(0, 14)));
        addSectionLabel(macroCardsContainer, "Today's Actual Intake");
        addStatCards(macroCardsContainer, MACRO_INTAKE_LABELS, macroCurrentValues, isMacroEditMode);
        macroCardsContainer.revalidate(); macroCardsContainer.repaint();
    }

    private JPanel createPersonalRecordsScreen() {
        AppScreen panel = createScreenShell("1RM / PRs", "pr");
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        JPanel tools = new JPanel(new BorderLayout(8, 0)); tools.setOpaque(false);
        prEditBtn = new RoundedButton("Edit", 10); prEditBtn.setPreferredSize(new Dimension(90, 32));
        tools.add(prEditBtn, BorderLayout.EAST); center.add(tools, BorderLayout.NORTH);
        prCardsContainer = new JPanel();
        prCardsContainer.setLayout(new BoxLayout(prCardsContainer, BoxLayout.Y_AXIS));
        prCardsContainer.setOpaque(false);
        JScrollPane scroll = new JScrollPane(prCardsContainer);
        styleScroll(scroll); enableVerticalDragScroll(scroll);
        center.add(scroll, BorderLayout.CENTER); panel.add(center, BorderLayout.CENTER);
        prEditBtn.addActionListener(e -> {
            isPrEditMode = !isPrEditMode;
            prEditBtn.setText(isPrEditMode ? "Done" : "Edit");
            prEditBtn.setBackground(isPrEditMode ? Theme.DANGER : Theme.PANEL_MID());
            prEditBtn.setForeground(isPrEditMode ? Color.WHITE : Theme.TEXT_MUTED());
            refreshRecordCards();
        });
        panel.setNavigateHook(this::refreshRecordCards); refreshRecordCards();
        return panel;
    }

    private void refreshRecordCards() {
        prCardsContainer.removeAll();
        prCardsContainer.add(Box.createRigidArea(new Dimension(0, 18)));
        if (personalRecords.isEmpty()) {
            prCardsContainer.add(createEmptyStateCard("No PRs yet", "Tap + to add a lift and your best weight."));
        } else {
            for (RecordData record : personalRecords) {
                prCardsContainer.add(buildRecordCard(record));
                prCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        prCardsContainer.revalidate(); prCardsContainer.repaint();
    }

    private JPanel buildRecordCard(RecordData record) {
        RoundedPanel card = new RoundedPanel(15, Theme.CARD_BG());
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(440, 58)); card.setPreferredSize(new Dimension(440, 58));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        card.add(transparentLabel(record.getName(), SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()), BorderLayout.WEST);
        if (isPrEditMode) {
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); btns.setOpaque(false);
            RoundedButton editBtn = new RoundedButton("Edit", 8);
            editBtn.setBackground(Theme.ACCENT_DARK); editBtn.setForeground(Color.WHITE);
            editBtn.setPreferredSize(new Dimension(50, 28));
            editBtn.addActionListener(e -> editRecordDialog(record)); btns.add(editBtn);
            RoundedButton delBtn = new RoundedButton("Del", 8);
            delBtn.setBackground(Theme.DANGER); delBtn.setForeground(Color.WHITE);
            delBtn.setPreferredSize(new Dimension(44, 28));
            delBtn.addActionListener(e -> { personalRecords.remove(record); refreshRecordCards(); save(); }); btns.add(delBtn);
            card.add(btns, BorderLayout.EAST);
        } else {
            String w = record.getWeight();
            if (w != null && !w.trim().isEmpty() && !w.matches(".*[a-zA-Z].*")) w = w.trim() + " kg";
            String display = (w == null || w.trim().isEmpty()) ? "— not set" : w;
            card.add(transparentLabel(display, SwingConstants.RIGHT, Theme.BODY, Theme.TEXT_MUTED()), BorderLayout.EAST);
        }
        return card;
    }

    private JPanel createHistoryScreen() {
        AppScreen screen = createScreenShell("History", "history");
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        historyExerciseSelect = new JComboBox<>();
        historyExerciseSelect.setBackground(Theme.FIELD_BG()); historyExerciseSelect.setForeground(Theme.TEXT());
        historyExerciseSelect.addActionListener(e -> updateHistoryChart());
        center.add(historyExerciseSelect, BorderLayout.NORTH);
        historyChart = new BarChartPanel();
        center.add(historyChart, BorderLayout.CENTER);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(() -> { refreshExerciseChoices(); updateHistoryChart(); });
        return screen;
    }

    private void refreshExerciseChoices() {
        Object prev = historyExerciseSelect.getSelectedItem();
        Set<String> names = new LinkedHashSet<>();
        for (ArrayList<ExerciseData> logs : datedWorkoutLogs.values())
            for (ExerciseData d : logs) names.add(d.getName());
        historyExerciseSelect.removeAllItems();
        for (String name : names) historyExerciseSelect.addItem(name);
        if (prev != null) historyExerciseSelect.setSelectedItem(prev);
    }

    private void updateHistoryChart() {
        if (historyChart == null || historyExerciseSelect.getSelectedItem() == null) {
            if (historyChart != null) historyChart.setData(new ArrayList<>(), new ArrayList<>(), "Completed exercise");
            return;
        }
        String exercise = historyExerciseSelect.getSelectedItem().toString();
        LocalDate today = LocalDate.now();
        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            double total = datedWorkoutLogs.getOrDefault(date, new ArrayList<>())
                    .stream().filter(d -> d.getName().equals(exercise))
                    .mapToDouble(this::estimateVolume).sum();
            values.add(total);
            labels.add(date.format(DateTimeFormatter.ofPattern("dd")));
        }
        historyChart.setData(values, labels, exercise);
    }

    private JPanel createBodyScreen() {
        AppScreen screen = createScreenShell("Body", "body");
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        RoundedPanel heightBox = new RoundedPanel(16, Theme.PANEL_DARK());
        heightBox.setLayout(new BorderLayout());
        heightBox.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        heightBox.setPreferredSize(new Dimension(420, 58));
        heightBox.add(transparentLabel("Height", SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()), BorderLayout.WEST);
        heightSummaryButton = new RoundedButton(heightCm > 0 ? String.format("%.0f cm", heightCm) : "Set height", 10);
        heightSummaryButton.setBackground(Theme.ACCENT); heightSummaryButton.setForeground(Color.WHITE);
        heightSummaryButton.setPreferredSize(new Dimension(112, 30));
        heightSummaryButton.addActionListener(e -> setHeightDialog());
        heightBox.add(heightSummaryButton, BorderLayout.EAST);
        center.add(heightBox, BorderLayout.NORTH);
        bodyChart = new LineChartPanel(); center.add(bodyChart, BorderLayout.CENTER);
        bodyLogPanel = new JPanel(); bodyLogPanel.setLayout(new BoxLayout(bodyLogPanel, BoxLayout.Y_AXIS)); bodyLogPanel.setOpaque(false);
        JScrollPane logScroll = new JScrollPane(bodyLogPanel);
        logScroll.setPreferredSize(new Dimension(420, 160));
        styleScroll(logScroll); enableVerticalDragScroll(logScroll);
        center.add(logScroll, BorderLayout.SOUTH);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(this::refreshBodyUI);
        return screen;
    }

    private void refreshBodyUI() {
        bodyEntries.sort(Comparator.comparing(BodyWeightEntry::getDate));
        bodyChart.setData(bodyEntries);
        if (heightSummaryButton != null)
            heightSummaryButton.setText(heightCm > 0 ? String.format("%.0f cm", heightCm) : "Set height");
        bodyLogPanel.removeAll();
        double hm = heightCm / 100.0;
        List<BodyWeightEntry> reversed = new ArrayList<>(bodyEntries);
        Collections.reverse(reversed);
        for (BodyWeightEntry entry : reversed) {
            String val = String.format("%.1f kg", entry.getWeightKg());
            val += hm > 0 ? String.format("  |  BMI %.1f", entry.getWeightKg()/(hm*hm)) : "  |  Set height for BMI";
            bodyLogPanel.add(new StatCardPanel(entry.getDate().toString(), val, false, ignored -> {}));
            bodyLogPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        bodyLogPanel.revalidate(); bodyLogPanel.repaint();
    }

    private JPanel createCardioScreen() {
        AppScreen screen = createScreenShell("Cardio", "cardio");
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        JLabel hdr = transparentLabel("Cardio Log", SwingConstants.LEFT, Theme.HEADER_FONT, Theme.TEXT());
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        center.add(hdr, BorderLayout.NORTH);
        cardioLogPanel = new JPanel(); cardioLogPanel.setLayout(new BoxLayout(cardioLogPanel, BoxLayout.Y_AXIS)); cardioLogPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(cardioLogPanel);
        styleScroll(scroll); enableVerticalDragScroll(scroll);
        center.add(scroll, BorderLayout.CENTER);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(this::refreshCardioUI);
        return screen;
    }

    private void refreshCardioUI() {
        if (cardioLogPanel == null) return;
        cardioLogPanel.removeAll();
        cardioLogPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        if (cardioEntries.isEmpty()) {
            cardioLogPanel.add(createEmptyStateCard("No cardio logged", "Tap + to log a session."));
        } else {
            List<CardioEntry> sorted = new ArrayList<>(cardioEntries);
            sorted.sort(Comparator.comparing(CardioEntry::getDate).reversed());
            for (CardioEntry entry : sorted) {
                cardioLogPanel.add(buildCardioCard(entry));
                cardioLogPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        cardioLogPanel.revalidate(); cardioLogPanel.repaint();
    }

    private JPanel buildCardioCard(CardioEntry entry) {
        RoundedPanel card = new RoundedPanel(15, Theme.CARD_BG());
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(440, 72)); card.setPreferredSize(new Dimension(440, 72));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS)); left.setOpaque(false);
        left.add(transparentLabel(entry.getType() + "  \u2022  " + entry.getDate(), SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()));
        left.add(Box.createRigidArea(new Dimension(0, 3)));
        String detail = String.format("%.1f km  |  %d min", entry.getDistanceKm(), entry.getDurationMinutes());
        if (entry.getDistanceKm() > 0 && entry.getDurationMinutes() > 0)
            detail += String.format("  |  %.1f min/km", entry.getPaceMinPerKm());
        if (entry.getNotes() != null && !entry.getNotes().isEmpty()) detail += "  |  " + entry.getNotes();
        left.add(transparentLabel(detail, SwingConstants.LEFT, Theme.SMALL, Theme.TEXT_MUTED()));
        card.add(left, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); btns.setOpaque(false);
        RoundedButton editBtn = new RoundedButton("Edit", 8);
        editBtn.setBackground(Theme.ACCENT_DARK); editBtn.setForeground(Color.WHITE);
        editBtn.setPreferredSize(new Dimension(50, 28));
        editBtn.addActionListener(e -> editCardioDialog(entry));
        btns.add(editBtn);
        RoundedButton delBtn = new RoundedButton("Del", 8);
        delBtn.setBackground(Theme.DANGER); delBtn.setForeground(Color.WHITE);
        delBtn.setPreferredSize(new Dimension(44, 28));
        delBtn.addActionListener(e -> { cardioEntries.remove(entry); refreshCardioUI(); save(); });
        btns.add(delBtn);
        card.add(btns, BorderLayout.EAST);
        return card;
    }

    private void editCardioDialog(CardioEntry entry) {
        String[] types = {"Run","Cycle","Row","Swim","Walk","Other"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        typeBox.setSelectedItem(entry.getType());
        JTextField distField     = new JTextField(String.valueOf(entry.getDistanceKm()));
        JTextField durationField = new JTextField(String.valueOf(entry.getDurationMinutes()));
        JTextField notesField    = new JTextField(entry.getNotes() == null ? "" : entry.getNotes());
        JPanel p = new JPanel(new GridLayout(4,2,10,10)); p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Type:"));               p.add(typeBox);
        p.add(new JLabel("Distance (km):"));      p.add(distField);
        p.add(new JLabel("Duration (minutes):")); p.add(durationField);
        p.add(new JLabel("Notes:"));   p.add(notesField);
        if (JOptionPane.showConfirmDialog(this, p, "Edit Cardio", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int idx = cardioEntries.indexOf(entry);
                if (idx >= 0) {
                    cardioEntries.set(idx, new CardioEntry(entry.getDate(),
                            (String) typeBox.getSelectedItem(),
                            Double.parseDouble(distField.getText().trim()),
                            Integer.parseInt(durationField.getText().trim()),
                            notesField.getText().trim()));
                    refreshCardioUI(); save();
                }
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter valid numbers."); }
        }
    }

    private JPanel createAssessmentScreen() {
        AppScreen screen = createScreenShell("Assessment", "assessment");
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);

        // Top: button + subtitle
        JPanel topSection = new JPanel(); topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS)); topSection.setOpaque(false);
        topSection.add(Box.createRigidArea(new Dimension(0, 8)));
        RoundedButton assessBtn = new RoundedButton("Analyse My Programme", 14);
        assessBtn.setBackground(Theme.ACCENT); assessBtn.setForeground(Color.WHITE);
        assessBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        assessBtn.setMaximumSize(new Dimension(360, 44)); assessBtn.setPreferredSize(new Dimension(360, 44));
        assessBtn.addActionListener(e -> runAssessment());
        topSection.add(assessBtn);
        topSection.add(Box.createRigidArea(new Dimension(0, 6)));
        JLabel sub = transparentLabel("Analyses your full weekly split — exercises, balance, volume and rest times.",
                SwingConstants.CENTER, Theme.SMALL, Theme.TEXT_MUTED());
        sub.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        topSection.add(sub);
        center.add(topSection, BorderLayout.NORTH);

        // Report card — takes all remaining space
        RoundedPanel reportCard = new RoundedPanel(18, Theme.CARD_BG());
        reportCard.setLayout(new BorderLayout());
        reportCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        assessorFeedback = new JTextArea("Tap \"Analyse My Programme\" to get a full breakdown.");
        assessorFeedback.setEditable(false);
        assessorFeedback.setLineWrap(true);
        assessorFeedback.setWrapStyleWord(true);
        assessorFeedback.setOpaque(false);
        assessorFeedback.setForeground(Theme.TEXT_MUTED());
        assessorFeedback.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane feedbackScroll = new JScrollPane(assessorFeedback);
        styleScroll(feedbackScroll);
        reportCard.add(feedbackScroll, BorderLayout.CENTER);

        center.add(reportCard, BorderLayout.CENTER);
        screen.add(center, BorderLayout.CENTER);
        return screen;
    }

    private void runAssessment() {
        if (masterExercises.isEmpty()) {
            assessorFeedback.setText("No exercises found.\n\nAdd exercises to your weekly routine first, then come back here for an analysis.");
            return;
        }

        Map<DayOfWeek, List<ExerciseData>> byDay = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) byDay.put(d, new ArrayList<>());
        for (ExerciseData ex : masterExercises) byDay.get(ex.getDayOfWeek()).add(ex);

        Map<String,Integer> setsByCat   = new HashMap<>();
        Map<String,Double>  volByCat    = new HashMap<>();
        int totalSets=0; double totalVol=0; int shortRest=0; int longRest=0;
        int trainingDays=0; double avgRestBetweenSets=0; int restCount=0;

        for (ExerciseData d : masterExercises) {
            String cat = classifyExercise(d.getName());
            int sets = getTargetSets(d); double vol = estimateVolume(d);
            setsByCat.merge(cat,sets,Integer::sum); volByCat.merge(cat,vol,Double::sum);
            totalSets+=sets; totalVol+=vol;
            avgRestBetweenSets+=d.getRestSeconds(); restCount++;
            if (d.getRestSeconds()<45) shortRest++;
            if (d.getRestSeconds()>180) longRest++;
        }
        for (List<ExerciseData> day : byDay.values()) if (!day.isEmpty()) trainingDays++;
        if (restCount>0) avgRestBetweenSets/=restCount;

        int push=setsByCat.getOrDefault("Push",0), pull=setsByCat.getOrDefault("Pull",0),
            legs=setsByCat.getOrDefault("Legs",0), arms=setsByCat.getOrDefault("Arms",0),
            core=setsByCat.getOrDefault("Core",0);
        int setsPerDay = trainingDays>0 ? totalSets/trainingDays : 0;

        int score=0;
        if (push>0) score+=10; if (pull>0) score+=10; if (legs>0) score+=10;
        if (push>0&&pull>0&&Math.abs(push-pull)<=3) score+=15;
        if (trainingDays>=3&&trainingDays<=5) score+=15;
        if (setsPerDay>=3&&setsPerDay<=6) score+=15;
        if (shortRest==0) score+=10; if (longRest==0) score+=5;
        if (core>0) score+=5; if (arms>0) score+=5;
        score=Math.min(100,score);

        String grade = score>=90?"A+":score>=80?"A":score>=70?"B+":score>=60?"B":score>=50?"C+":"C";

        StringBuilder r = new StringBuilder();
        r.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        r.append(String.format("  SPLIT SCORE:  %d%%  (%s)\n", score, grade));
        r.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        r.append(String.format("Training days/week:  %d\n", trainingDays));
        r.append(String.format("Total weekly sets:   %d\n", totalSets));
        r.append(String.format("Sets per session:    %d\n", setsPerDay));
        r.append(String.format("Weekly volume:       %.0f\n", totalVol));
        r.append(String.format("Avg rest time:       %.0fs\n\n", avgRestBetweenSets));

        r.append("MUSCLE GROUP BREAKDOWN\n");
        r.append("─────────────────────\n");
        for (String cat : new String[]{"Push","Pull","Legs","Arms","Core","Other"}) {
            int s=setsByCat.getOrDefault(cat,0);
            if (s==0) continue;
            double v=volByCat.getOrDefault(cat,0.0);
            int bar=Math.min(20,(int)(s/Math.max(1.0,totalSets)*20));
            r.append(String.format("%-6s  %s  %d sets\n", cat, "\u2588".repeat(bar)+"\u2591".repeat(20-bar), s));
        }
        r.append("\n");

        r.append("DAY BY DAY\n");
        r.append("──────────\n");
        for (Map.Entry<DayOfWeek, List<ExerciseData>> e : byDay.entrySet()) {
            String dayName = e.getKey().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if (restDays.contains(e.getKey())) {
                r.append(String.format("%-4s  \uD83D\uDECC Rest day\n", dayName));
            } else if (e.getValue().isEmpty()) {
                r.append(String.format("%-4s  — no exercises\n", dayName));
            } else {
                Set<String> cats = new LinkedHashSet<>();
                for (ExerciseData ex : e.getValue()) cats.add(classifyExercise(ex.getName()));
                r.append(String.format("%-4s  %d exercises  [%s]\n", dayName, e.getValue().size(), String.join(", ",cats)));
            }
        }
        r.append("\n");

        r.append("COACH NOTES\n");
        r.append("───────────\n");
        if (push==0) r.append("✗ No push work. Add pressing or chest movements.\n");
        else if (push>0) r.append("✓ Push work present.\n");
        if (pull==0) r.append("✗ No pull work. Add rows, pulldowns, or deadlifts.\n");
        else r.append("✓ Pull work present.\n");
        if (legs==0) r.append("✗ No leg work. Add squats, hinges, or lunges.\n");
        else r.append("✓ Leg work present.\n");
        if (push>0&&pull>0) {
            int diff=Math.abs(push-pull);
            if (diff<=3) r.append("✓ Push/pull balance is good.\n");
            else r.append(String.format("⚠ Push/pull imbalance: %d vs %d sets. Aim to keep within 3.\n",push,pull));
        }
        if (trainingDays<3) r.append("⚠ Only "+trainingDays+" training day(s). Most programmes use 3–5.\n");
        else if (trainingDays>5) r.append("⚠ "+trainingDays+" training days is high. Ensure adequate recovery.\n");
        else r.append("✓ Training frequency ("+trainingDays+" days) is solid.\n");
        if (setsPerDay<3) r.append("⚠ Fewer than 3 sets per session. Volume may be too low.\n");
        else if (setsPerDay>8) r.append("⚠ More than 8 sets per session. Watch fatigue and junk volume.\n");
        else r.append("✓ Sets per session look manageable.\n");
        if (shortRest>0) r.append("⚠ "+shortRest+" exercise(s) have rest under 45s. Heavy compound lifts need more.\n");
        if (longRest>0) r.append("⚠ "+longRest+" exercise(s) have rest over 3 min. Fine for max effort, watch session length.\n");
        if (core==0) r.append("→ Consider adding core work (planks, carries, crunches).\n");
        if (score>=85) r.append("✓ Strong programme structure overall.\n");

        assessorFeedback.setText(r.toString());
        assessorFeedback.setCaretPosition(0);
    }

    private JPanel createSettingsScreen() {
        AppScreen screen = createScreenShell("Settings", "settings");
        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setOpaque(false);
        JScrollPane scroll = new JScrollPane(center); styleScroll(scroll); enableVerticalDragScroll(scroll);

        center.add(Box.createRigidArea(new Dimension(0, 14)));
        addSectionLabel(center, "Appearance");
        center.add(buildSettingsToggleRow("Dark Mode", "Switch between dark and light theme", Theme.isDark(), toggled -> {
            Theme.setDark(toggled); AppSettings.setDarkMode(toggled);
            SwingUtilities.updateComponentTreeUI(GymApp.this); GymApp.this.repaint();
        }));
        center.add(Box.createRigidArea(new Dimension(0, 14)));
        addSectionLabel(center, "Workout Defaults");
        center.add(buildSettingsInfoRow("Default Rest Time", "90s (set per exercise)"));
        center.add(Box.createRigidArea(new Dimension(0, 8)));
        center.add(buildSettingsInfoRow("Exercise Visibility", "Added date onwards only"));
        center.add(Box.createRigidArea(new Dimension(0, 8)));
        center.add(buildSettingsInfoRow("Streak Logic", "Breaks only if day has exercises & none done"));
        center.add(Box.createRigidArea(new Dimension(0, 14)));
        addSectionLabel(center, "Data");
        center.add(buildSettingsActionRow("Save All Data", "Writes to ~/.ironpulse/", Theme.ACCENT, () -> {
            save(); JOptionPane.showMessageDialog(GymApp.this, "Saved to ~/.ironpulse/");
        }));
        center.add(Box.createRigidArea(new Dimension(0, 8)));
        center.add(buildSettingsActionRow("Clear Today's Exercises", "Removes exercises added today", Theme.DANGER, () -> {
            int confirm = JOptionPane.showConfirmDialog(GymApp.this,
                    "Remove all exercises added on " + currentSelectedDate + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                masterExercises.removeIf(ex -> ex.getAddedDate().equals(currentSelectedDate));
                datedWorkoutLogs.remove(currentSelectedDate);
                loggedSetsByDate.remove(currentSelectedDate);
                save(); JOptionPane.showMessageDialog(GymApp.this, "Cleared.");
            }
        }));
        center.add(Box.createRigidArea(new Dimension(0, 14)));
        addSectionLabel(center, "About");
        center.add(buildSettingsInfoRow("App", "IronPulse Gym Tracker"));
        center.add(Box.createRigidArea(new Dimension(0, 8)));
        center.add(buildSettingsInfoRow("Data location", "~/.ironpulse/"));
        center.add(Box.createRigidArea(new Dimension(0, 8)));
        center.add(buildSettingsInfoRow("Auto-save", "On every change"));
        center.add(Box.createRigidArea(new Dimension(0, 20)));

        screen.add(scroll, BorderLayout.CENTER);
        return screen;
    }

    private JPanel buildSettingsToggleRow(String label, String subtitle, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        RoundedPanel row = new RoundedPanel(14, Theme.CARD_BG());
        row.setLayout(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(440, 66)); row.setPreferredSize(new Dimension(440, 66));
        row.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JPanel textCol = new JPanel(); textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS)); textCol.setOpaque(false);
        textCol.add(transparentLabel(label, SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()));
        textCol.add(Box.createRigidArea(new Dimension(0,2)));
        textCol.add(transparentLabel(subtitle, SwingConstants.LEFT, Theme.SMALL, Theme.TEXT_MUTED()));
        row.add(textCol, BorderLayout.WEST);

        final boolean[] state = {initial};
        final float[]   knobX = {initial ? 1f : 0f};

        JPanel toggleWidget = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int tw = getWidth(), th = getHeight();
                int trackH = 28, trackY = (th - trackH) / 2;
                int trackW = tw;

                Color trackOff = new Color(60, 65, 75);
                Color trackOn  = Theme.ACCENT_DARK;
                Color track    = state[0]
                        ? new Color(trackOn.getRed(), trackOn.getGreen(), trackOn.getBlue())
                        : (Theme.isDark() ? trackOff : new Color(200, 210, 218));

                g2.setColor(track);
                g2.fillRoundRect(0, trackY, trackW, trackH, trackH, trackH);

                if (state[0]) {
                    g2.setColor(Theme.ACCENT_GLOW);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, trackY, trackW-1, trackH-1, trackH, trackH);
                } else {
                    g2.setColor(Theme.BORDER());
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, trackY, trackW-1, trackH-1, trackH, trackH);
                }

                int knobDiam = trackH - 6;
                int knobMinX = 3, knobMaxX = trackW - knobDiam - 3;
                int knobPx   = knobMinX + (int)((knobMaxX - knobMinX) * knobX[0]);
                int knobPy   = trackY + 3;

                g2.setPaint(new GradientPaint(knobPx, knobPy,
                        state[0] ? new Color(255,220,80) : Color.WHITE,
                        knobPx, knobPy + knobDiam,
                        state[0] ? new Color(255,170,30) : new Color(230,230,240)));
                g2.fillOval(knobPx, knobPy, knobDiam, knobDiam);

                g2.setColor(state[0] ? new Color(200,130,0,80) : new Color(0,0,0,30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(knobPx, knobPy, knobDiam, knobDiam);

                int cx = knobPx + knobDiam/2, cy = knobPy + knobDiam/2;
                if (state[0]) {
                    g2.setColor(new Color(200, 130, 0, 180));
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int sr = knobDiam/2 + 3;
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        int x1 = (int)(cx + (sr-2)*Math.cos(a)), y1 = (int)(cy + (sr-2)*Math.sin(a));
                        int x2 = (int)(cx + (sr+2)*Math.cos(a)), y2 = (int)(cy + (sr+2)*Math.sin(a));
                        g2.drawLine(x1, y1, x2, y2);
                    }
                } else {
                    g2.setColor(new Color(150, 160, 180, 160));
                    g2.setStroke(new BasicStroke(1.5f));
                    int mr = knobDiam/2 - 3;
                    g2.drawArc(cx - mr, cy - mr, mr*2, mr*2, 30, 300);
                }

                String icon = state[0] ? "\u2600" : "\u263D";
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(state[0] ? new Color(255,220,80,200) : new Color(180,190,210,200));
                FontMetrics fm = g2.getFontMetrics();
                int iconX = state[0] ? 6 : trackW - fm.stringWidth(icon) - 5;
                g2.drawString(icon, iconX, trackY + trackH/2 + fm.getAscent()/2 - 1);

                g2.dispose();
            }
        };
        toggleWidget.setOpaque(false);
        toggleWidget.setPreferredSize(new Dimension(68, 34));
        toggleWidget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        javax.swing.Timer[] anim = {null};
        toggleWidget.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                state[0] = !state[0];
                float target = state[0] ? 1f : 0f;
                if (anim[0] != null) anim[0].stop();
                anim[0] = new javax.swing.Timer(16, ev -> {
                    float diff = target - knobX[0];
                    if (Math.abs(diff) < 0.03f) {
                        knobX[0] = target;
                        ((javax.swing.Timer)ev.getSource()).stop();
                    } else {
                        knobX[0] += diff * 0.25f;
                    }
                    toggleWidget.repaint();
                });
                anim[0].start();
                onChange.accept(state[0]);
            }
        });

        row.add(toggleWidget, BorderLayout.EAST);
        return row;
    }

    private JPanel buildSettingsInfoRow(String label, String value) {
        RoundedPanel row = new RoundedPanel(14, Theme.CARD_BG());
        row.setLayout(new BorderLayout());
        row.setMaximumSize(new Dimension(440, 52)); row.setPreferredSize(new Dimension(440, 52));
        row.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        row.add(transparentLabel(label, SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()), BorderLayout.WEST);
        row.add(transparentLabel(value, SwingConstants.RIGHT, Theme.SMALL, Theme.TEXT_MUTED()), BorderLayout.EAST);
        return row;
    }

    private JPanel buildSettingsActionRow(String label, String subtitle, Color accentColor, Runnable action) {
        RoundedPanel row = new RoundedPanel(14, Theme.CARD_BG());
        row.setLayout(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(440, 66)); row.setPreferredSize(new Dimension(440, 66));
        row.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JPanel textCol = new JPanel(); textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS)); textCol.setOpaque(false);
        textCol.add(transparentLabel(label, SwingConstants.LEFT, Theme.BODY_BOLD, Theme.TEXT()));
        textCol.add(Box.createRigidArea(new Dimension(0,2)));
        textCol.add(transparentLabel(subtitle, SwingConstants.LEFT, Theme.SMALL, Theme.TEXT_MUTED()));
        row.add(textCol, BorderLayout.WEST);
        RoundedButton btn = new RoundedButton("Go", 10);
        btn.setBackground(accentColor); btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(52, 30));
        btn.addActionListener(e -> action.run());
        row.add(btn, BorderLayout.EAST);
        return row;
    }

    private JPanel createExerciseDetailScreen() {
        AppScreen screen = createScreenShell("Exercise", "workout");
        JPanel center = new JPanel(); center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        RoundedPanel detailCard = new RoundedPanel(20, Theme.CARD_BG());
        detailCard.setLayout(new BoxLayout(detailCard, BoxLayout.Y_AXIS));
        detailCard.setBorder(BorderFactory.createEmptyBorder(24, 22, 24, 22));
        detailCard.setMaximumSize(new Dimension(430, 250)); detailCard.setPreferredSize(new Dimension(430, 250));
        exerciseDetailTitle = new JLabel("Select an exercise");
        exerciseDetailTitle.setForeground(Theme.TEXT()); exerciseDetailTitle.setFont(Theme.TITLE_FONT);
        exerciseDetailTitle.setAlignmentX(JPanel.CENTER_ALIGNMENT); exerciseDetailTitle.setOpaque(false);
        exerciseDetailStats = new JLabel("Open an exercise from the workout list.");
        exerciseDetailStats.setForeground(Theme.TEXT_MUTED()); exerciseDetailStats.setFont(Theme.BODY);
        exerciseDetailStats.setAlignmentX(JPanel.CENTER_ALIGNMENT); exerciseDetailStats.setOpaque(false);
        restTimerLabel = new JLabel("Rest timer ready");
        restTimerLabel.setForeground(Theme.ACCENT_2); restTimerLabel.setFont(new Font("Segoe UI",Font.BOLD,30));
        restTimerLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT); restTimerLabel.setOpaque(false);
        JButton logSetButton = new RoundedButton("Log Set", 16);
        logSetButton.setBackground(Theme.ACCENT); logSetButton.setForeground(Color.WHITE);
        logSetButton.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        logSetButton.setMaximumSize(new Dimension(230, 46)); logSetButton.setPreferredSize(new Dimension(230, 46));
        logSetButton.addActionListener(e -> logSetAndStartRest());
        JButton backButton = new RoundedButton("Back to Workout", 14);
        backButton.setBackground(Theme.PANEL_MID()); backButton.setForeground(Theme.TEXT());
        backButton.setAlignmentX(JPanel.CENTER_ALIGNMENT); backButton.setMaximumSize(new Dimension(230, 42));
        backButton.addActionListener(e -> navigateTo("WORKOUT"));
        detailCard.add(exerciseDetailTitle); detailCard.add(Box.createRigidArea(new Dimension(0,12)));
        detailCard.add(exerciseDetailStats); detailCard.add(Box.createRigidArea(new Dimension(0,24)));
        detailCard.add(restTimerLabel); detailCard.add(Box.createRigidArea(new Dimension(0,18)));
        detailCard.add(logSetButton);
        center.add(Box.createRigidArea(new Dimension(0,30))); center.add(detailCard);
        center.add(Box.createRigidArea(new Dimension(0,18))); center.add(backButton);
        screen.add(center, BorderLayout.CENTER);
        screen.setNavigateHook(this::refreshExerciseDetail);
        return screen;
    }

    private void refreshExerciseDetail() {
        if (selectedExercise == null) {
            exerciseDetailTitle.setText("Select an exercise");
            exerciseDetailStats.setText("Open an exercise from the workout list.");
            restTimerLabel.setText("Rest timer ready"); return;
        }
        exerciseDetailTitle.setText(selectedExercise.getName());
        exerciseDetailStats.setText(formatExerciseSummary(selectedExercise) + "  |  Sets logged " + getLoggedSets(selectedExercise));
        if (remainingRestSeconds <= 0) restTimerLabel.setText("Rest timer ready");
    }

    private void logSetAndStartRest() {
        if (selectedExercise == null) return;
        if (isExerciseCompletedOnDate(selectedExercise, currentSelectedDate)) {
            JOptionPane.showMessageDialog(this, "Uncheck the exercise to log more sets.");
            return;
        }
        int targetSets=getTargetSets(selectedExercise), loggedSets=getLoggedSets(selectedExercise);
        loggedSets++;
        setLoggedSets(selectedExercise, loggedSets);
        if (loggedSets >= targetSets) {
            stopRestTimer(); remainingRestSeconds=0;
            setExerciseHistoryComplete(selectedExercise, true);
            if (streakPanel != null) streakPanel.update(masterExercises, datedWorkoutLogs, restDays);
            refreshExerciseDetail(); save();
            JOptionPane.showMessageDialog(this, selectedExercise.getName()+" complete. Nice work."); return;
        }
        remainingRestSeconds=selectedExercise.getRestSeconds();
        stopRestTimer(); refreshExerciseDetail(); updateRestTimerLabel();
        restTimer = new javax.swing.Timer(1000, e -> {
            remainingRestSeconds--;
            updateRestTimerLabel();
            if (remainingRestSeconds<=0) { stopRestTimer(); restTimerLabel.setText("Rest complete"); }
        });
        restTimer.start();
    }

    private void stopRestTimer() { if (restTimer != null) { restTimer.stop(); restTimer = null; } }
    private void updateRestTimerLabel() {
        restTimerLabel.setText(String.format("Rest %d:%02d",
                Math.max(0,remainingRestSeconds)/60, Math.max(0,remainingRestSeconds)%60));
    }

    private void addExerciseDialog() {
        JTextField nameField  = new JTextField();
        JTextField weightField= new JTextField();
        JTextField setsField  = new JTextField("3");
        JTextField repsField  = new JTextField("10");
        JTextField restField  = new JTextField("90");
        JPanel p = new JPanel(new GridLayout(5,2,10,10));
        p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Exercise name:")); p.add(nameField);
        p.add(new JLabel("Weight (kg/lbs):")); p.add(weightField);
        p.add(new JLabel("Sets:"));             p.add(setsField);
        p.add(new JLabel("Reps:"));             p.add(repsField);
        p.add(new JLabel("Rest (seconds):"));   p.add(restField);
        if (JOptionPane.showConfirmDialog(this, p, "Add Exercise", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            masterExercises.add(new ExerciseData(name,
                    weightField.getText().trim(),
                    parsePositiveInt(setsField.getText(),3,1)+"x"+parsePositiveInt(repsField.getText(),10,1),
                    parsePositiveInt(restField.getText(),90,5), currentSelectedDate));
            refreshWorkoutScreen(); save();
        }
    }

    private void editExerciseDialog(ExerciseData data) {
        String[] parts = data.getReps()==null ? new String[]{"3","10"} : data.getReps().split("x");
        JTextField weightField = new JTextField(data.getWeight());
        JTextField setsField   = new JTextField(parts.length>0?parts[0]:"3");
        JTextField repsField   = new JTextField(parts.length>1?parts[1]:"10");
        JTextField restField   = new JTextField(String.valueOf(data.getRestSeconds()));
        JPanel p = new JPanel(new GridLayout(4,2,10,10));
        p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Weight (kg/lbs):")); p.add(weightField);
        p.add(new JLabel("Sets:"));             p.add(setsField);
        p.add(new JLabel("Reps:"));             p.add(repsField);
        p.add(new JLabel("Rest (seconds):"));   p.add(restField);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Edit: "+data.getName(), p},
                "Edit Exercise", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            data.setWeight(weightField.getText().trim());
            data.setReps(parsePositiveInt(setsField.getText(),3,1)+"x"+parsePositiveInt(repsField.getText(),10,1));
            data.setRestSeconds(parsePositiveInt(restField.getText(),90,5));
            refreshExerciseListUI(); save();
        }
    }

    private void addRecordDialog() {
        JTextField nameField=new JTextField(), weightField=new JTextField();
        JPanel p=new JPanel(new GridLayout(2,2,10,10)); p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Lift name:")); p.add(nameField);
        p.add(new JLabel("Best weight (kg/lbs):")); p.add(weightField);
        if (JOptionPane.showConfirmDialog(this,p,"Add PR",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION
                && !nameField.getText().trim().isEmpty()) {
            personalRecords.add(new RecordData(nameField.getText().trim(),weightField.getText().trim()));
            refreshRecordCards(); save();
        }
    }

    private void editRecordDialog(RecordData record) {
        JTextField weightField=new JTextField(record.getWeight());
        JPanel p=new JPanel(new GridLayout(1,2,10,10));
        p.add(new JLabel("Best weight (kg/lbs):")); p.add(weightField);
        if (JOptionPane.showConfirmDialog(this,new Object[]{"Edit: "+record.getName(),p},
                "Edit PR",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            record.setWeight(weightField.getText().trim()); refreshRecordCards(); save();
        }
    }

    private void addBodyEntryDialog() {
        JTextField dateField=new JTextField(LocalDate.now().toString()), weightField=new JTextField();
        if (JOptionPane.showConfirmDialog(this,
                new Object[]{"Date (YYYY-MM-DD):", dateField, "Bodyweight (kg or lbs):", weightField},
                "Add Bodyweight", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            try {
                bodyEntries.add(new BodyWeightEntry(LocalDate.parse(dateField.getText().trim()),
                        Double.parseDouble(weightField.getText().trim())));
                refreshBodyUI(); save();
            } catch (RuntimeException ex) { JOptionPane.showMessageDialog(this,"Enter a valid date and number."); }
        }
    }

    private void setHeightDialog() {
        String input=JOptionPane.showInputDialog(this,"Height in cm:",heightCm>0?String.valueOf((int)heightCm):"");
        if (input==null) return;
        try { heightCm=Double.parseDouble(input.trim()); AppSettings.setHeightCm(heightCm); refreshBodyUI(); }
        catch (NumberFormatException ignored) { JOptionPane.showMessageDialog(this,"Enter a valid height."); }
    }

    private void addCardioDialog() {
        String[] types = {"Run","Cycle","Row","Swim","Walk","Other"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        JTextField distField     = new JTextField("0");
        JTextField durationField = new JTextField("30");
        JTextField notesField    = new JTextField();
        JPanel p = new JPanel(new GridLayout(4,2,10,10)); p.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
        p.add(new JLabel("Type:"));               p.add(typeBox);
        p.add(new JLabel("Distance (km):"));      p.add(distField);
        p.add(new JLabel("Duration (minutes):")); p.add(durationField);
        p.add(new JLabel("Notes:"));   p.add(notesField);
        if (JOptionPane.showConfirmDialog(this, p, "Log Cardio", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                cardioEntries.add(new CardioEntry(currentSelectedDate, (String) typeBox.getSelectedItem(),
                        Double.parseDouble(distField.getText().trim()),
                        Integer.parseInt(durationField.getText().trim()),
                        notesField.getText().trim()));
                refreshCardioUI(); save();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter valid numbers for distance and duration."); }
        }
    }

    private void setExerciseHistoryComplete(ExerciseData data, boolean completed) {
        ArrayList<ExerciseData> logs=datedWorkoutLogs.computeIfAbsent(currentSelectedDate, d->new ArrayList<>());
        if (completed) {
            if (!logs.contains(data)) logs.add(data);
        } else {
            logs.remove(data);
            setLoggedSets(data, 0);
            stopRestTimer(); remainingRestSeconds=0;
            if (restTimerLabel != null) restTimerLabel.setText("Rest timer ready");
        }
        if ("HISTORY".equals(activeScreenName)) { refreshExerciseChoices(); updateHistoryChart(); }
    }

    private boolean isExerciseCompletedOnDate(ExerciseData data, LocalDate date) {
        ArrayList<ExerciseData> logs=datedWorkoutLogs.get(date);
        return logs!=null && logs.contains(data);
    }

    private void removeExerciseFromAllCompletedDates(ExerciseData data) {
        datedWorkoutLogs.values().forEach(l->l.remove(data));
        loggedSetsByDate.values().forEach(m->m.remove(data));
    }

    private int getTargetSets(ExerciseData data) {
        String[] parts=data.getReps()==null?new String[0]:data.getReps().toLowerCase().split("x");
        return parts.length==0?1:parsePositiveInt(parts[0],1,1);
    }

    private int getLoggedSets(ExerciseData data) {
        Map<ExerciseData,Integer> m=loggedSetsByDate.get(currentSelectedDate);
        return m==null?0:m.getOrDefault(data,0);
    }

    private void setLoggedSets(ExerciseData data, int sets) {
        loggedSetsByDate.computeIfAbsent(currentSelectedDate, d->new HashMap<>()).put(data,sets);
    }

    private double estimateVolume(ExerciseData data) {
        double weight=parseFirstNumber(data.getWeight());
        String[] parts=data.getReps()==null?new String[0]:data.getReps().toLowerCase().split("x");
        double sets=parts.length>1?parseFirstNumber(parts[0]):1;
        double reps=parts.length>1?parseFirstNumber(parts[1]):parseFirstNumber(data.getReps());
        if (weight<=0) return Math.max(1,sets)*Math.max(1,reps);
        return weight*Math.max(1,sets)*Math.max(1,reps);
    }

    private String classifyExercise(String name) {
        if (name==null) return "Other";
        String ex=name.toLowerCase();
        if (ex.contains("bench")||ex.contains("press")||ex.contains("chest")||ex.contains("push")||ex.contains("shoulder")) return "Push";
        if (ex.contains("row")||ex.contains("pull")||ex.contains("lat")||ex.contains("deadlift")||ex.contains("back")) return "Pull";
        if (ex.contains("squat")||ex.contains("leg")||ex.contains("calf")||ex.contains("lunge")||ex.contains("ham")||ex.contains("glute")) return "Legs";
        if (ex.contains("curl")||ex.contains("tricep")||ex.contains("bicep")||ex.contains("extension")) return "Arms";
        if (ex.contains("plank")||ex.contains("crunch")||ex.contains("abs")||ex.contains("core")) return "Core";
        return "Other";
    }

    private String formatExerciseSummary(ExerciseData data) {
        String[] p=data.getReps()==null?new String[0]:data.getReps().toLowerCase().split("x");
        String sets=p.length>0&&!p[0].trim().isEmpty()?p[0].trim():"?";
        String reps=p.length>1&&!p[1].trim().isEmpty()?p[1].trim():"?";
        return formatWeight(data)+"  |  "+sets+" sets x "+reps+" reps  |  Rest "+data.getRestSeconds()+"s";
    }

    private String formatExerciseSummaryCompact(ExerciseData data) {
        String reps=(data.getReps()==null||data.getReps().trim().isEmpty())?"?x?":data.getReps().trim();
        return formatWeight(data)+" | "+reps+" | "+data.getRestSeconds()+"s";
    }

    private String formatWeight(ExerciseData data) {
        if (data.getWeight()==null||data.getWeight().trim().isEmpty()) return "- kg";
        return data.getWeight().trim()+" kg";
    }

    private void addSectionLabel(JPanel container, String text) {
        JLabel label=transparentLabel(text, SwingConstants.LEFT, Theme.HEADER_FONT, Theme.TEXT());
        label.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        container.add(label); container.add(Box.createRigidArea(new Dimension(0,8)));
    }

    private void addStatCards(JPanel container, String[] labels, String[] values, boolean editMode) {
        for (int i=0; i<labels.length; i++) {
            final int idx=i;
            container.add(new StatCardPanel(labels[idx],values[idx],editMode,v->values[idx]=v));
            container.add(Box.createRigidArea(new Dimension(0,10)));
        }
    }

    private void updateEditButton(JButton button, boolean editing) {
        button.setText(editing?"Save":"Edit Stats");
        button.setBackground(editing?Theme.ACCENT:Theme.PANEL_MID());
        button.setForeground(editing?Color.WHITE:Theme.TEXT_MUTED());
    }

    private JPanel createEmptyStateCard(String title, String message) {
        RoundedPanel card=new RoundedPanel(20,Theme.PANEL_DARK());
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20,18,20,18));
        card.setMaximumSize(new Dimension(420,100)); card.setPreferredSize(new Dimension(420,100));
        JLabel titleLabel=transparentLabel(title, SwingConstants.CENTER, Theme.HEADER_FONT, Theme.TEXT());
        titleLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        JLabel msgLabel=transparentLabel(message, SwingConstants.CENTER, Theme.BODY, Theme.TEXT_MUTED());
        msgLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        card.add(titleLabel); card.add(Box.createRigidArea(new Dimension(0,6)));
        card.add(msgLabel);
        return card;
    }

    private JCheckBox createHistoryCheckBox() {
        JCheckBox cb=new JCheckBox() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int size=Math.min(getWidth(),getHeight())-6, x=(getWidth()-size)/2, y=(getHeight()-size)/2;
                g2.setPaint(Theme.verticalGradient(isSelected()?Theme.ACCENT:Theme.PANEL_MID(),size,size));
                g2.fillRoundRect(x,y,size,size,8,8);
                g2.setColor(isSelected()?Theme.ACCENT_GLOW:Theme.BORDER()); g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x,y,size,size,8,8);
                if (isSelected()) {
                    g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g2.drawLine(x+5,y+size/2,x+size/2-1,y+size-6); g2.drawLine(x+size/2-1,y+size-6,x+size-5,y+5);
                }
                g2.dispose();
            }
        };
        cb.setOpaque(false); cb.setContentAreaFilled(false); cb.setBorderPainted(false); cb.setFocusPainted(false);
        cb.setPreferredSize(new Dimension(26,26));
        return cb;
    }

    private void styleScroll(JScrollPane scroll) {
        scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(new Color(0,0,0,0));
        scroll.setBackground(new Color(0,0,0,0));
    }

    private void enableVerticalDragScroll(JScrollPane scroll) {
        MouseAdapter drag=new MouseAdapter() {
            private Point last;
            @Override public void mousePressed(MouseEvent e) { last=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),scroll.getViewport()); }
            @Override public void mouseDragged(MouseEvent e) {
                if (last==null) return;
                Point cur=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),scroll.getViewport());
                JViewport vp=scroll.getViewport(); Point pos=vp.getViewPosition();
                pos.y+=last.y-cur.y;
                pos.y=Math.max(0,Math.min(pos.y,Math.max(0,vp.getView().getHeight()-vp.getHeight())));
                vp.setViewPosition(pos); last=cur;
            }
            @Override public void mouseReleased(MouseEvent e) { last=null; }
        };
        scroll.getViewport().addMouseListener(drag); scroll.getViewport().addMouseMotionListener(drag);
        scroll.getViewport().getView().addMouseListener(drag); scroll.getViewport().getView().addMouseMotionListener(drag);
    }

    private void installHorizontalDragScroll(JPanel panel, JScrollPane scroll) {
        MouseAdapter drag=new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragStartPoint=e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStartPoint==null) return;
                JViewport vp=scroll.getViewport(); Point pos=vp.getViewPosition();
                pos.x+=dragStartPoint.x-e.getX();
                pos.x=Math.max(0,Math.min(pos.x,panel.getWidth()-vp.getWidth()));
                vp.setViewPosition(pos);
            }
        };
        panel.addMouseListener(drag); panel.addMouseMotionListener(drag);
        scroll.getViewport().addMouseListener(drag); scroll.getViewport().addMouseMotionListener(drag);
        for (Component c : panel.getComponents()) { c.addMouseListener(drag); c.addMouseMotionListener(drag); }
    }

    private int parsePositiveInt(String text, int fallback, int minimum) {
        try { return Math.max(minimum,Integer.parseInt(text.trim())); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private double parseFirstNumber(String text) {
        String cleaned=text==null?"":text.replaceAll("[^0-9.]"," ").trim();
        if (cleaned.isEmpty()) return 0;
        try { return Double.parseDouble(cleaned.split("\\s+")[0]); }
        catch (NumberFormatException ex) { return 0; }
    }

    private String cleanNumberText(String text) {
        return text==null?"":text.replaceAll("[^0-9.]","").trim();
    }

    private static class AppScreen extends JPanel implements Screen {
        private Runnable navigateHook = () -> {};
        AppScreen(BorderLayout layout) { super(layout); }
        void setNavigateHook(Runnable hook) { this.navigateHook=hook; }
        @Override public void onNavigateTo() { navigateHook.run(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();

            // Solid base
            g2.setColor(Theme.BG_DARK());
            g2.fillRect(0, 0, w, h);

            // Subtle dot grid
            Color dot = Theme.isDark() ? new Color(255,255,255,12) : new Color(0,0,0,8);
            g2.setColor(dot);
            for (int x=14; x<w; x+=28) for (int y=14; y<h; y+=28) g2.fillOval(x-1,y-1,2,2);

            // Accent glow blobs
            int ar=Theme.ACCENT.getRed(), ag=Theme.ACCENT.getGreen(), ab=Theme.ACCENT.getBlue();
            int a2r=Theme.ACCENT_2.getRed(), a2g=Theme.ACCENT_2.getGreen(), a2b=Theme.ACCENT_2.getBlue();
            g2.setColor(new Color(ar,ag,ab,Theme.isDark()?22:16));
            g2.fillOval(-80, h/2-100, 300, 340);
            g2.setColor(new Color(a2r,a2g,a2b,Theme.isDark()?16:10));
            g2.fillOval(w-200, -80, 300, 260);

            // Thin accent line under header
            g2.setPaint(new GradientPaint(0,80,new Color(ar,ag,ab,Theme.isDark()?90:60),w,80,new Color(ar,ag,ab,0)));
            g2.fillRect(0, 80, w, 2);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GymApp().setVisible(true));
    }
}
