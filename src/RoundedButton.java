import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedButton extends JButton {
    private final int radius;
    private boolean glowing;

    public RoundedButton(String label, int radius) {
        super(label);
        this.radius = radius;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(Theme.SMALL_BOLD);
        setBackground(Theme.PANEL_MID);
        setForeground(Theme.TEXT);
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = glowing ? 3 : 1;

        g2.setPaint(Theme.verticalGradient(getBackground(), getWidth(), getHeight()));
        g2.fill(new RoundRectangle2D.Double(inset, inset, getWidth() - inset * 2.0, getHeight() - inset * 2.0, radius, radius));

        if (glowing) {
            g2.setColor(Theme.ACCENT_GLOW);
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 4.0, getHeight() - 4.0, radius, radius));
        }

        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}