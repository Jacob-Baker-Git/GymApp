import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedPanel extends JPanel {

    private final int cornerRadius;
    private Color     backgroundColor;
    private boolean   selected;

    public RoundedPanel(int radius, Color bg) {
        this.cornerRadius    = radius;
        this.backgroundColor = bg;
        setOpaque(false);
    }

    public void setBackgroundColor(Color bg) { this.backgroundColor = bg; repaint(); }
    public void setSelectedGlow(boolean sel) { this.selected = sel; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int    inset = selected ? 3 : 1;
        double w     = getWidth()  - inset * 2.0;
        double h     = getHeight() - inset * 2.0;
        int    cr    = Scale.dp(cornerRadius);

        // Shadow — lighter in light mode
        int shadowAlpha = Theme.isDark() ? (selected ? 60 : 36) : (selected ? 28 : 14);
        g2.setColor(new Color(0, 0, 0, shadowAlpha));
        g2.fill(new RoundRectangle2D.Double(inset + 1, inset + 4, w - 2, h - 3, cr, cr));

        // Body
        g2.setPaint(Theme.verticalGradient(backgroundColor, getWidth(), getHeight()));
        g2.fill(new RoundRectangle2D.Double(inset, inset, w, h, cr, cr));

        // Border
        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(new RoundRectangle2D.Double(inset + 0.5, inset + 0.5, w - 1, h - 1, cr, cr));

        // Selection glow
        if (selected) {
            g2.setColor(Theme.ACCENT_GLOW);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(1.5, 1.5, getWidth() - 3.0, getHeight() - 3.0, cr, cr));
        }
        g2.dispose();
    }
}
