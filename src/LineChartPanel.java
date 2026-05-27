import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LineChartPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");
    private static final int PAD_LEFT = 42, PAD_RIGHT = 24, PAD_TOP = 54, PAD_BOTTOM = 38;

    private List<BodyWeightEntry> entries = new ArrayList<>();

    public LineChartPanel() { setOpaque(false); setPreferredSize(new java.awt.Dimension(420, 260)); }

    public void setData(List<BodyWeightEntry> entries) { this.entries = new ArrayList<>(entries); repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), w, h));
        g2.fillRoundRect(0, 0, w, h, 18, 18);
        g2.setColor(Theme.BORDER());
        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
        g2.setColor(Theme.TEXT()); g2.setFont(Theme.BODY_BOLD);
        g2.drawString("Bodyweight trend", 18, 28);

        if (entries.size() < 2) {
            g2.setFont(Theme.BODY); g2.setColor(Theme.TEXT_MUTED());
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Add two entries to draw a trend.";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose(); return;
        }

        int chartW = w - PAD_LEFT - PAD_RIGHT, chartH = h - PAD_TOP - PAD_BOTTOM;
        double min = entries.stream().mapToDouble(BodyWeightEntry::getWeightKg).min().orElse(0);
        double max = entries.stream().mapToDouble(BodyWeightEntry::getWeightKg).max().orElse(1);
        if (max - min < 1.0) { max += 0.5; min -= 0.5; }

        g2.setColor(Theme.shift(Theme.PANEL_MID(), 8));
        for (int i = 0; i < 4; i++) {
            int y = PAD_TOP + chartH * i / 3;
            g2.drawLine(PAD_LEFT, y, w - PAD_RIGHT, y);
        }

        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int lastX = 0, lastY = 0, n = entries.size();
        for (int i = 0; i < n; i++) {
            int x = PAD_LEFT + (n == 1 ? 0 : i * chartW / (n - 1));
            int y = PAD_TOP + chartH - (int) Math.round(((entries.get(i).getWeightKg() - min) / (max - min)) * chartH);
            if (i > 0) g2.drawLine(lastX, lastY, x, y);
            g2.fillOval(x - 4, y - 4, 8, 8);
            lastX = x; lastY = y;
        }

        g2.setFont(Theme.SMALL); g2.setColor(Theme.TEXT_MUTED());
        g2.drawString(String.format("%.1f kg", max), 12, PAD_TOP + 5);
        g2.drawString(String.format("%.1f kg", min), 12, PAD_TOP + chartH);
        String first = entries.get(0).getDate().format(DATE_FMT);
        String last  = entries.get(n - 1).getDate().format(DATE_FMT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(first, PAD_LEFT, h - 16);
        g2.drawString(last, w - PAD_RIGHT - fm.stringWidth(last), h - 16);
        g2.dispose();
    }
}
