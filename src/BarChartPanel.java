import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class BarChartPanel extends JPanel {

    private static final int PAD_LEFT = 52, PAD_RIGHT = 16, PAD_TOP = 48, PAD_BOTTOM = 36;
    private static final int BAR_GAP  = 3;

    private List<Double> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private String       title  = "Volume";

    public BarChartPanel() {
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(420, 300));
    }

    public void setData(List<Double> values, List<String> labels, String title) {
        this.values = new ArrayList<>(values);
        this.labels = new ArrayList<>(labels);
        this.title  = title;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Background card
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), w, h));
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));
        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w-1, h-1, 20, 20));

        // Title
        g2.setFont(Theme.BODY_BOLD);
        g2.setColor(Theme.TEXT());
        g2.drawString(title + "  —  past 2 weeks", 16, 28);

        if (values.isEmpty()) {
            g2.setFont(Theme.BODY); g2.setColor(Theme.TEXT_MUTED());
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Complete exercises to see history.";
            g2.drawString(msg, (w - fm.stringWidth(msg))/2, h/2);
            g2.dispose(); return;
        }

        int chartW = w - PAD_LEFT - PAD_RIGHT;
        int chartH = h - PAD_TOP  - PAD_BOTTOM;
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (max < 1) max = 1;

        int    n      = values.size();
        int    barW   = Math.max(6, chartW / n - BAR_GAP);
        int    gridN  = 4;

        // Y-axis scale + horizontal grid lines
        g2.setFont(Theme.SMALL);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= gridN; i++) {
            int    y   = PAD_TOP + chartH * i / gridN;
            double val = max * (gridN - i) / gridN;
            String lbl = val >= 1000 ? String.format("%.1fk", val/1000)
                       : val > 0     ? String.format("%.0f",   val)
                       : "0";
            g2.setColor(Theme.TEXT_MUTED());
            g2.drawString(lbl, PAD_LEFT - fm.stringWidth(lbl) - 5, y + fm.getAscent()/2 - 1);

            // Dashed grid line
            float[] dash = {4f, 4f};
            g2.setColor(new Color(Theme.shift(Theme.PANEL_MID(), 10).getRGB() & 0x00FFFFFF | 0x50000000, true));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dash, 0));
            if (i > 0) g2.drawLine(PAD_LEFT, y, w - PAD_RIGHT, y);
        }

        // Y-axis vertical line
        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);

        // Bars
        int latestIdx = n - 1;
        for (int i = 0; i < n; i++) {
            int    x    = PAD_LEFT + i * chartW / n + BAR_GAP;
            double v    = values.get(i);
            int    barH = (int) Math.round((v / max) * (chartH - 2));

            if (barH > 0) {
                int y = PAD_TOP + chartH - barH;

                // Subtle shadow
                g2.setColor(new Color(0, 0, 0, 35));
                g2.fill(new RoundRectangle2D.Double(x + 2, y + 3, barW, barH, 8, 8));

                // Bar gradient: latest bar uses gold, others green
                boolean isLatest = (i == latestIdx);
                Color   top      = isLatest ? Theme.ACCENT_2        : Theme.ACCENT;
                Color   bot      = isLatest ? new Color(200, 130, 0) : Theme.ACCENT_DARK;
                g2.setPaint(new GradientPaint(x, y, top, x, y + barH, bot));
                g2.fill(new RoundRectangle2D.Double(x, y, barW, barH, 8, 8));

                // Highlight rim on top of bar
                g2.setPaint(new GradientPaint(x, y, new Color(255,255,255,60), x, y + Math.min(barH, 14), new Color(255,255,255,0)));
                g2.fill(new RoundRectangle2D.Double(x+1, y+1, barW-2, Math.min(barH-1, 12), 6, 6));

                // Value label above bar if tall enough
                if (barH > 22) {
                    String valStr = v >= 1000 ? String.format("%.0fk", v/1000) : String.format("%.0f", v);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.setColor(new Color(255,255,255,200));
                    FontMetrics vfm = g2.getFontMetrics();
                    g2.drawString(valStr, x + (barW - vfm.stringWidth(valStr))/2, y + 11);
                }
            }

            // X-axis date label
            g2.setFont(Theme.SMALL);
            g2.setColor(i == latestIdx ? Theme.ACCENT_2 : Theme.TEXT_MUTED());
            String lbl = labels.get(i);
            g2.drawString(lbl, x + (barW - fm.stringWidth(lbl))/2, h - 8);
        }

        g2.dispose();
    }
}
