import javax.swing.JPanel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class StreakPanel extends JPanel {

    private int streak        = 0;
    private int totalWorkouts = 0;

    public StreakPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(440, 54));
        setMaximumSize(new Dimension(440, 54));
    }

    public void update(ArrayList<ExerciseData> master,
                       Map<LocalDate, ArrayList<ExerciseData>> logs,
                       Set<DayOfWeek> restDays) {
        streak = 0;
        totalWorkouts = 0;

        LocalDate today = LocalDate.now();

        // --- Is today complete? ---
        ArrayList<ExerciseData> todayPlanned = getPlanned(master, today);
        boolean todayIsRest   = restDays.contains(today.getDayOfWeek());
        boolean todayHasWork  = !todayPlanned.isEmpty();
        boolean todayComplete = todayHasWork && isDayComplete(todayPlanned, logs.get(today));
        boolean todayMissed   = todayHasWork && !todayIsRest && !todayComplete;

        // Count total from history (doesn't affect streak number)
        for (int i = 1; i <= 730; i++) {
            LocalDate d = today.minusDays(i);
            ArrayList<ExerciseData> p = getPlanned(master, d);
            if (!p.isEmpty() && !restDays.contains(d.getDayOfWeek()) && isDayComplete(p, logs.get(d)))
                totalWorkouts++;
        }
        if (todayComplete) totalWorkouts++;

        // --- Streak ---
        // Rule: streak is ONLY based on today.
        // If today is missed → streak = 0.
        // If today is complete → streak = 1 + count of consecutive completed workout days going backwards.
        // Past completions count ONLY for continuing the backward chain from today — they cannot raise streak independently.
        // Rest days and days with no exercises don't break the backward chain.

        if (todayMissed) { repaint(); return; }
        if (!todayComplete) { repaint(); return; }

        streak = 1; // today is done

        // Walk backwards
        for (int i = 1; i <= 730; i++) {
            LocalDate d = today.minusDays(i);
            DayOfWeek dow = d.getDayOfWeek();
            if (restDays.contains(dow)) continue; // skip rest
            ArrayList<ExerciseData> p = getPlanned(master, d);
            if (p.isEmpty()) continue; // skip no-work days
            if (isDayComplete(p, logs.get(d))) {
                streak++;
            } else {
                break; // chain broken
            }
        }

        repaint();
    }

    private ArrayList<ExerciseData> getPlanned(ArrayList<ExerciseData> master, LocalDate date) {
        ArrayList<ExerciseData> result = new ArrayList<>();
        for (ExerciseData ex : master) {
            LocalDate added = ex.getAddedDate();
            if (added.isAfter(date)) continue;
            long diff = java.time.temporal.ChronoUnit.DAYS.between(added, date);
            if (diff % 7 == 0) result.add(ex);
        }
        return result;
    }

    private boolean isDayComplete(ArrayList<ExerciseData> planned, ArrayList<ExerciseData> done) {
        if (done == null || planned.isEmpty()) return false;
        return done.containsAll(planned);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Card background
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), w, h));
        g2.fillRoundRect(0, 0, w, h, 14, 14);
        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w-1, h-1, 14, 14);

        // Progress bar
        int barX=12, barY=h-10, barH=3, barW=w-24;
        g2.setColor(Theme.PANEL_MID());
        g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
        if (streak > 0) {
            double ratio = Math.min(1.0, streak / 30.0);
            int filled = Math.max(barH, (int)(ratio * barW));
            g2.setPaint(new GradientPaint(barX, 0, Theme.ACCENT_DARK, barX + filled, 0, Theme.ACCENT));
            g2.fillRoundRect(barX, barY, filled, barH, barH, barH);
        }

        // Flame + streak
        int midY = (h - barH) / 2 + 5;
        g2.setFont(Theme.BODY_BOLD);
        g2.setColor(streak > 0 ? Theme.ACCENT : Theme.TEXT_MUTED());
        String streakStr = streak > 0 ? "\uD83D\uDD25  " + streak + " day streak" : "Complete today to start a streak";
        g2.drawString(streakStr, 14, midY);

        // Total workouts right-aligned
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.TEXT_MUTED());
        String totalStr = totalWorkouts + " sessions total";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(totalStr, w - fm.stringWidth(totalStr) - 14, midY);

        g2.dispose();
    }
}
