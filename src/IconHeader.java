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
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK, getWidth(), getHeight()));
        g2.fillRoundRect(0, 4, getWidth(), 54, 20, 20);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 4, getWidth() - 1, 53, 20, 20);
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, 44, 44));
        g2.fillRoundRect(8, 10, 42, 42, 16, 16);
        g2.setColor(Theme.ACCENT_GLOW);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(9, 11, 40, 40, 16, 16);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x = 10;
        int y = 14;
        g2.setColor(Color.WHITE);
        if ("workout".equals(type)) {
            g2.drawLine(x + 4, y + 19, x + 35, y + 19);
            g2.fillRoundRect(x + 1, y + 12, 8, 15, 5, 5);
            g2.fillRoundRect(x + 30, y + 12, 8, 15, 5, 5);
        } else if ("macro".equals(type)) {
            g2.drawOval(x + 5, y + 7, 27, 27);
            g2.draw(new Arc2D.Double(x + 8, y + 10, 21, 21, 35, 110, Arc2D.OPEN));
            g2.fillOval(x + 18, y + 18, 5, 5);
            g2.drawLine(x + 21, y + 20, x + 31, y + 10);
        } else if ("pr".equals(type)) {
            g2.drawLine(x + 8, y + 33, x + 30, y + 33);
            g2.drawLine(x + 19, y + 9, x + 19, y + 33);
            g2.drawLine(x + 10, y + 18, x + 19, y + 9);
            g2.drawLine(x + 28, y + 18, x + 19, y + 9);
        } else if ("history".equals(type)) {
            g2.fillRoundRect(x + 5, y + 26, 6, 10, 4, 4);
            g2.fillRoundRect(x + 16, y + 18, 6, 18, 4, 4);
            g2.fillRoundRect(x + 27, y + 10, 6, 26, 4, 4);
            g2.drawLine(x + 4, y + 36, x + 35, y + 36);
        } else if ("body".equals(type)) {
            g2.drawLine(x + 6, y + 34, x + 6, y + 10);
            g2.drawLine(x + 6, y + 34, x + 34, y + 34);
            g2.drawLine(x + 6, y + 26, x + 15, y + 22);
            g2.drawLine(x + 15, y + 22, x + 23, y + 25);
            g2.drawLine(x + 23, y + 25, x + 34, y + 12);
            g2.fillOval(x + 31, y + 9, 6, 6);
        } else if ("assessment".equals(type)) {
            g2.drawOval(x + 7, y + 8, 25, 25);
            g2.drawLine(x + 19, y + 12, x + 19, y + 21);
            g2.drawLine(x + 19, y + 21, x + 27, y + 17);
            g2.drawLine(x + 11, y + 36, x + 28, y + 36);
        } else {
            g2.drawLine(x + 6, y + 34, x + 6, y + 10);
            g2.drawLine(x + 6, y + 34, x + 34, y + 34);
            g2.drawLine(x + 6, y + 27, x + 16, y + 22);
            g2.drawLine(x + 16, y + 22, x + 25, y + 25);
            g2.drawLine(x + 25, y + 25, x + 34, y + 12);
        }

        g2.setFont(Theme.TITLE_FONT);
        g2.setColor(Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, 66, 24 + fm.getAscent() / 2);
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("IronPulse training dashboard", 67, 50);
        g2.dispose();
    }
}