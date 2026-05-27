import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class RoundedButton extends JButton {

    private final int radius;
    private boolean   glowing;
    private boolean   hovering;

    public RoundedButton(String label, int radius) {
        super(label);
        this.radius = radius;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(Theme.SMALL_BOLD);
        setBackground(Theme.PANEL_MID());
        setForeground(Theme.TEXT());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovering = true;  repaint(); }
            @Override public void mouseExited (MouseEvent e) { hovering = false; repaint(); }
        });
    }

    public void setGlowing(boolean glowing) { this.glowing = glowing; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int    inset = glowing ? 3 : 1;
        double w     = getWidth()  - inset * 2.0;
        double h     = getHeight() - inset * 2.0;
        Color  bg    = hovering ? Theme.shift(getBackground(), 16) : getBackground();

        g2.setColor(new Color(0, 0, 0, 55));
        g2.fill(new RoundRectangle2D.Double(inset + 1, inset + 4, w - 2, h - 3, radius, radius));

        g2.setPaint(Theme.verticalGradient(bg, getWidth(), getHeight()));
        g2.fill(new RoundRectangle2D.Double(inset, inset, w, h, radius, radius));

        g2.setColor(new Color(255, 255, 255, hovering ? 45 : 24));
        g2.setStroke(new BasicStroke(1));
        g2.draw(new RoundRectangle2D.Double(inset + 0.5, inset + 0.5, w - 1, h - 1, radius, radius));

        if (glowing) {
            g2.setColor(Theme.ACCENT_GLOW);
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 4.0, getHeight() - 4.0, radius, radius));
        }

        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
        int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(getText(), tx, ty);
        g2.dispose();
    }
}
