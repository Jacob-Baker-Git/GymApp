import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;

public class IconHeader extends JComponent {

    private final String title;
    private final String type;
    private Runnable     onSettingsClick;
    private boolean      gearHover;

    private static final int GEAR_W  = 36;
    private static final int GEAR_H  = 36;
    private static final int BAR_GAP = GEAR_W + 10;

    public IconHeader(String title, String type) {
        this.title = title;
        this.type  = type;
        setPreferredSize(new Dimension(440, 64));
        setMinimumSize(new Dimension(320, 64));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (isOverGear(e.getX(), e.getY()) && onSettingsClick != null)
                    onSettingsClick.run();
            }
            @Override public void mouseExited(MouseEvent e) {
                gearHover = false; repaint();
                setCursor(Cursor.getDefaultCursor());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                boolean over = isOverGear(e.getX(), e.getY());
                if (over != gearHover) { gearHover = over; repaint(); }
                setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void setOnSettingsClick(Runnable r) { this.onSettingsClick = r; }

    private boolean isOverGear(int mx, int my) {
        int gx = getWidth() - GEAR_W - 4;
        int gy = (getHeight() - GEAR_H) / 2;
        return mx >= gx && mx <= gx + GEAR_W && my >= gy && my <= gy + GEAR_H;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();

        int barW = w - BAR_GAP - 4;
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), barW, getHeight()));
        g2.fillRoundRect(0, 4, barW, 54, 20, 20);
        g2.setColor(Theme.BORDER());
        g2.drawRoundRect(0, 4, barW - 1, 53, 20, 20);

        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, 44, 44));
        g2.fillRoundRect(8, 10, 42, 42, 16, 16);
        g2.setColor(Theme.ACCENT_GLOW);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(9, 11, 40, 40, 16, 16);

        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        drawIcon(g2, 10, 14);

        g2.setFont(Theme.TITLE_FONT);
        g2.setColor(Theme.TEXT());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, 66, 24 + fm.getAscent() / 2);
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.TEXT_MUTED());
        g2.drawString("IronPulse training dashboard", 67, 50);

        drawGearButton(g2, w);
        g2.dispose();
    }

    private void drawGearButton(Graphics2D g2, int w) {
        int gx = w - GEAR_W - 4;
        int gy = (getHeight() - GEAR_H) / 2 + 2;
        int cx = gx + GEAR_W / 2;
        int cy = gy + GEAR_H / 2;

        g2.setPaint(Theme.verticalGradient(
                gearHover ? Theme.shift(Theme.ACCENT_DARK, 8) : Theme.PANEL_MID(),
                GEAR_W, GEAR_H));
        g2.fillRoundRect(gx, gy, GEAR_W, GEAR_H, 10, 10);
        g2.setColor(gearHover ? Theme.ACCENT_GLOW : Theme.BORDER());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(gx, gy, GEAR_W - 1, GEAR_H - 1, 10, 10);

        Color iconCol = gearHover ? Color.WHITE : Theme.TEXT_MUTED();
        drawGearIcon(g2, cx, cy, iconCol);
    }

    private void drawGearIcon(Graphics2D g2, int cx, int cy, Color col) {
        int teeth    = 8;
        int outerR   = 10;
        int innerR   = 7;
        int toothLen = 4;
        int toothW   = (int)(2 * Math.PI * outerR / teeth * 0.45);

        GeneralPath gear = new GeneralPath();
        for (int i = 0; i < teeth; i++) {
            double a1 = 2 * Math.PI * i / teeth - Math.PI / teeth * 0.6;
            double a2 = 2 * Math.PI * i / teeth - Math.PI / teeth * 0.2;
            double a3 = 2 * Math.PI * i / teeth + Math.PI / teeth * 0.2;
            double a4 = 2 * Math.PI * i / teeth + Math.PI / teeth * 0.6;

            double ix1 = cx + innerR * Math.cos(a1), iy1 = cy + innerR * Math.sin(a1);
            double ox1 = cx + (outerR + toothLen/2) * Math.cos(a2), oy1 = cy + (outerR + toothLen/2) * Math.sin(a2);
            double ox2 = cx + (outerR + toothLen/2) * Math.cos(a3), oy2 = cy + (outerR + toothLen/2) * Math.sin(a3);
            double ix2 = cx + innerR * Math.cos(a4), iy2 = cy + innerR * Math.sin(a4);

            if (i == 0) gear.moveTo(ix1, iy1);
            else gear.lineTo(ix1, iy1);
            gear.lineTo(ox1, oy1);
            gear.lineTo(ox2, oy2);
            gear.lineTo(ix2, iy2);

            double nextA = 2 * Math.PI * (i + 1) / teeth - Math.PI / teeth * 0.6;
            gear.curveTo(
                cx + innerR * Math.cos(a4 + 0.1), cy + innerR * Math.sin(a4 + 0.1),
                cx + innerR * Math.cos(nextA - 0.1), cy + innerR * Math.sin(nextA - 0.1),
                cx + innerR * Math.cos(nextA), cy + innerR * Math.sin(nextA));
        }
        gear.closePath();

        g2.setColor(col);
        g2.fill(gear);

        g2.setColor(gearHover ? Theme.ACCENT_DARK : Theme.PANEL_DARK());
        g2.fillOval(cx - 4, cy - 4, 9, 9);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - 4, cy - 4, 9, 9);
    }

    private void drawIcon(Graphics2D g2, int x, int y) {
        switch (type) {
            case "workout":
                g2.drawLine(x+4,y+19,x+35,y+19);
                g2.fillRoundRect(x+1,y+12,8,15,5,5);
                g2.fillRoundRect(x+30,y+12,8,15,5,5);
                break;
            case "macro":
                g2.drawOval(x+5,y+7,27,27);
                g2.draw(new Arc2D.Double(x+8,y+10,21,21,35,110,Arc2D.OPEN));
                g2.fillOval(x+18,y+18,5,5);
                g2.drawLine(x+21,y+20,x+31,y+10);
                break;
            case "pr":
                g2.drawLine(x+8,y+33,x+30,y+33);
                g2.drawLine(x+19,y+9,x+19,y+33);
                g2.drawLine(x+10,y+18,x+19,y+9);
                g2.drawLine(x+28,y+18,x+19,y+9);
                break;
            case "history":
                g2.fillRoundRect(x+5,y+26,6,10,4,4);
                g2.fillRoundRect(x+16,y+18,6,18,4,4);
                g2.fillRoundRect(x+27,y+10,6,26,4,4);
                g2.drawLine(x+4,y+36,x+35,y+36);
                break;
            case "body":
                g2.drawLine(x+6,y+34,x+6,y+10);
                g2.drawLine(x+6,y+34,x+34,y+34);
                g2.drawLine(x+6,y+26,x+15,y+22);
                g2.drawLine(x+15,y+22,x+23,y+25);
                g2.drawLine(x+23,y+25,x+34,y+12);
                g2.fillOval(x+31,y+9,6,6);
                break;
            case "assessment":
                g2.drawOval(x+7,y+8,25,25);
                g2.drawLine(x+19,y+12,x+19,y+21);
                g2.drawLine(x+19,y+21,x+27,y+17);
                g2.drawLine(x+11,y+36,x+28,y+36);
                break;
            case "cardio":
                g2.drawOval(x+8,y+8,22,22);
                g2.drawLine(x+19,y+8,x+19,y+4);
                g2.drawLine(x+19,y+19,x+26,y+14);
                g2.drawLine(x+5,y+30,x+33,y+30);
                break;
            case "settings":
                drawGearIcon(g2, x+19, y+19, Color.WHITE);
                break;
            default:
                g2.drawLine(x+6,y+34,x+6,y+10);
                g2.drawLine(x+6,y+34,x+34,y+34);
                g2.drawLine(x+6,y+27,x+16,y+22);
                g2.drawLine(x+16,y+22,x+25,y+25);
                g2.drawLine(x+25,y+25,x+34,y+12);
                break;
        }
    }
}
