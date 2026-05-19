import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

public class IconHeader extends JComponent {
    private final String title;
    private final String type;

    public IconHeader(String title, String type) {
        this.title = title;
        this.type = type;
        setPreferredSize(new Dimension(440, 64));
        setMinimumSize(new Dimension(320, 64));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x = 12;
        int y = 14;
        if ("workout".equals(type)) {
            g2.drawLine(x + 3, y + 19, x + 36, y + 19);
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 10, 8, 18, 4, 4));
            g2.draw(new RoundRectangle2D.Double(x + 31, y + 10, 8, 18, 4, 4));
        } else if ("macro".equals(type)) {
            g2.drawOval(x + 4, y + 6, 30, 30);
            g2.draw(new Arc2D.Double(x + 7, y + 9, 24, 24, 35, 110, Arc2D.OPEN));
            g2.drawLine(x + 19, y + 21, x + 32, y + 8);
        } else if ("pr".equals(type)) {
            g2.drawLine(x + 8, y + 34, x + 30, y + 34);
            g2.drawLine(x + 19, y + 8, x + 19, y + 34);
            g2.drawLine(x + 10, y + 17, x + 19, y + 8);
            g2.drawLine(x + 28, y + 17, x + 19, y + 8);
        } else if ("history".equals(type)) {
            g2.fillRect(x + 4, y + 26, 6, 11);
            g2.fillRect(x + 16, y + 17, 6, 20);
            g2.fillRect(x + 28, y + 9, 6, 28);
        } else {
            g2.drawLine(x + 6, y + 34, x + 6, y + 10);
            g2.drawLine(x + 6, y + 34, x + 36, y + 34);
            g2.drawLine(x + 6, y + 27, x + 16, y + 22);
            g2.drawLine(x + 16, y + 22, x + 25, y + 25);
            g2.drawLine(x + 25, y + 25, x + 36, y + 12);
        }

        g2.setFont(Theme.TITLE_FONT);
        g2.setColor(Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, 62, 24 + fm.getAscent() / 2);
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("IronPulse", 63, 50);
        g2.dispose();
    }
}