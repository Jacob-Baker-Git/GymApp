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

        int    inset = selected ? 4 : 1;
        double w     = getWidth()  - inset * 2.0;
        double h     = getHeight() - inset * 2.0;

        g2.setColor(new Color(0, 0, 0, selected ? 70 : 45));
        g2.fill(new RoundRectangle2D.Double(inset + 2, inset + 5, w - 4, h - 4, cornerRadius, cornerRadius));

        g2.setPaint(Theme.verticalGradient(backgroundColor, getWidth(), getHeight()));
        g2.fill(new RoundRectangle2D.Double(inset, inset, w, h, cornerRadius, cornerRadius));

        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(new RoundRectangle2D.Double(inset + 0.5, inset + 0.5, w - 1, h - 1, cornerRadius, cornerRadius));

        if (selected) {
            g2.setColor(Theme.ACCENT_GLOW);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 4.0, getHeight() - 4.0, cornerRadius, cornerRadius));
        }
        g2.dispose();
    }
}
