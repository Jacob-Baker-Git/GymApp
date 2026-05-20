import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

public class BarChartPanel extends JPanel {
    private List<Double> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private String title = "Volume";

    public BarChartPanel() {
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(420, 360));
    }

    public void setData(List<Double> values, List<String> labels, String title) {
        this.values = new ArrayList<>(values);
        this.labels = new ArrayList<>(labels);
        this.title = title;
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
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.BODY_BOLD);
        g2.drawString(title + " volume - past 2 weeks", 18, 28);

        if (values.isEmpty()) {
            drawCentered(g2, "Add exercises to see volume history.", w, h);
            g2.dispose();
            return;
        }

        double max = 1;
        for (double value : values) {
            max = Math.max(max, value);
        }
        int left = 34;
        int right = 18;
        int top = 50;
        int bottom = 42;
        int chartW = w - left - right;
        int chartH = h - top - bottom;
        g2.setColor(Theme.shift(Theme.PANEL_MID, 8));
        g2.setStroke(new BasicStroke(1));
        for (int i = 0; i < 4; i++) {
            int y = top + (chartH * i / 3);
            g2.drawLine(left, y, w - right, y);
        }
        int gap = 5;
        int barW = Math.max(8, (chartW / values.size()) - gap);
        for (int i = 0; i < values.size(); i++) {
            int x = left + i * chartW / values.size() + gap / 2;
            int barH = (int) Math.round((values.get(i) / max) * (chartH - 8));
            int y = top + chartH - barH;
            g2.setPaint(Theme.verticalGradient(i == values.size() - 1 ? Theme.ACCENT_2 : Theme.ACCENT, barW, barH));
            g2.fillRoundRect(x, y, barW, barH, 8, 8);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.SMALL);
            String label = labels.get(i);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x + (barW - fm.stringWidth(label)) / 2, h - 17);
        }
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(String.format("Peak %.0f", max), left, top - 8);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int w, int h) {
        g2.setFont(Theme.BODY);
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
    }
}