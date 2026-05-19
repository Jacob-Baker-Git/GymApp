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
    private List<BodyWeightEntry> entries = new ArrayList<>();

    public LineChartPanel() {
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(420, 260));
    }

    public void setData(List<BodyWeightEntry> entries) {
        this.entries = new ArrayList<>(entries);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK, w, h));
        g2.fillRoundRect(0, 0, w, h, 18, 18);
        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.BODY_BOLD);
        g2.drawString("Bodyweight trend", 18, 28);

        if (entries.size() < 2) {
            drawCentered(g2, "Add two entries to draw a trend.", w, h);
            g2.dispose();
            return;
        }

        double min = Double.MAX_VALUE;
        double max = 0;
        for (BodyWeightEntry entry : entries) {
            min = Math.min(min, entry.weightKg);
            max = Math.max(max, entry.weightKg);
        }
        if (Math.abs(max - min) < 0.1) {
            max += 1;
            min -= 1;
        }

        int left = 42;
        int right = 24;
        int top = 54;
        int bottom = 38;
        int chartW = w - left - right;
        int chartH = h - top - bottom;
        g2.setColor(Theme.shift(Theme.PANEL_MID, 8));
        for (int i = 0; i < 4; i++) {
            int y = top + chartH * i / 3;
            g2.drawLine(left, y, w - right, y);
        }

        int lastX = 0;
        int lastY = 0;
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < entries.size(); i++) {
            BodyWeightEntry entry = entries.get(i);
            int x = left + (entries.size() == 1 ? 0 : i * chartW / (entries.size() - 1));
            int y = top + chartH - (int) Math.round(((entry.weightKg - min) / (max - min)) * chartH);
            if (i > 0) {
                g2.drawLine(lastX, lastY, x, y);
            }
            g2.fillOval(x - 4, y - 4, 8, 8);
            lastX = x;
            lastY = y;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(String.format("%.1f kg", max), 12, top + 5);
        g2.drawString(String.format("%.1f kg", min), 12, top + chartH);
        g2.drawString(entries.get(0).date.format(formatter), left, h - 16);
        String lastLabel = entries.get(entries.size() - 1).date.format(formatter);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(lastLabel, w - right - fm.stringWidth(lastLabel), h - 16);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int w, int h) {
        g2.setFont(Theme.BODY);
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
    }
}