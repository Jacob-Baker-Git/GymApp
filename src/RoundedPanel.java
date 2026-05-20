import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedPanel extends JPanel {
    private final int cornerRadius;
    private Color backgroundColor;
    private boolean selected;

    public RoundedPanel(int radius, Color bg) {
        this.cornerRadius = radius;
        this.backgroundColor = bg;
        setOpaque(false);
    }

    public void setBackgroundColor(Color bg) {
        this.backgroundColor = bg;
        repaint();
    }

    public void setSelectedGlow(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = selected ? 4 : 1;
        graphics.setColor(new Color(0, 0, 0, selected ? 70 : 45));
        graphics.fill(new RoundRectangle2D.Double(inset + 2, inset + 5, getWidth() - inset * 2.0 - 4, getHeight() - inset * 2.0 - 4, cornerRadius, cornerRadius));
        graphics.setPaint(Theme.verticalGradient(backgroundColor, getWidth(), getHeight()));
        graphics.fill(new RoundRectangle2D.Double(inset, inset, getWidth() - inset * 2.0, getHeight() - inset * 2.0, cornerRadius, cornerRadius));
        graphics.setColor(Theme.BORDER);
        graphics.setStroke(new BasicStroke(1.1f));
        graphics.draw(new RoundRectangle2D.Double(inset + 0.5, inset + 0.5, getWidth() - inset * 2.0 - 1, getHeight() - inset * 2.0 - 1, cornerRadius, cornerRadius));
        if (selected) {
            graphics.setColor(Theme.ACCENT_GLOW);
            graphics.setStroke(new BasicStroke(2.5f));
            graphics.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 4.0, getHeight() - 4.0, cornerRadius, cornerRadius));
        }
        graphics.dispose();
    }
}