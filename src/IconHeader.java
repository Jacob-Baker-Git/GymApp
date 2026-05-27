import javax.swing.JComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;

public class IconHeader extends JComponent {

    private final String title;
    private final String type;
    private Runnable     onSettingsClick;
    private boolean      gearHover;

    public IconHeader(String title, String type) {
        this.title = title;
        this.type  = type;
        setPreferredSize(new Dimension(440, Scale.dp(64)));
        setMinimumSize(new Dimension(280, Scale.dp(56)));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (isOverGear(e.getX(), e.getY()) && onSettingsClick != null)
                    onSettingsClick.run();
            }
            @Override public void mouseExited(MouseEvent e) {
                gearHover = false; repaint(); setCursor(Cursor.getDefaultCursor());
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

    private int gearW()  { return Scale.dp(36); }
    private int gearH()  { return Scale.dp(36); }
    private int gearGap(){ return gearW() + Scale.dp(8); }

    private boolean isOverGear(int mx, int my) {
        int gx = getWidth() - gearW() - Scale.dp(4);
        int gy = (getHeight() - gearH()) / 2;
        return mx >= gx && mx <= gx + gearW() && my >= gy && my <= gy + gearH();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w  = getWidth();
        int h  = getHeight();
        int cr = Scale.dp(18);
        int barW = w - gearGap() - Scale.dp(6);

        // Header bar — in light mode use a card-style white/light background
        g2.setPaint(Theme.verticalGradient(Theme.PANEL_DARK(), barW, h));
        g2.fillRoundRect(0, Scale.dp(4), barW, h - Scale.dp(6), cr, cr);
        g2.setColor(Theme.BORDER());
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawRoundRect(0, Scale.dp(4), barW - 1, h - Scale.dp(7), cr, cr);

        // Icon badge
        int bx = Scale.dp(8), by = Scale.dp(10), bs = Scale.dp(40), bcr = Scale.dp(14);
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, bs, bs));
        g2.fillRoundRect(bx, by, bs, bs, bcr, bcr);
        g2.setColor(Theme.ACCENT_GLOW);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(bx + 1, by + 1, bs - 2, bs - 2, bcr, bcr);

        g2.setStroke(new BasicStroke(Scale.dp(2.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        drawIcon(g2, bx + Scale.dp(2), by + Scale.dp(4));

        // Title
        g2.setFont(Theme.titleFont());
        g2.setColor(Theme.TEXT());
        FontMetrics fm = g2.getFontMetrics();
        int textX = bx + bs + Scale.dp(10);
        g2.drawString(title, textX, Scale.dp(22) + fm.getAscent() / 2);

        // Subtitle
        g2.setFont(Theme.small());
        g2.setColor(Theme.TEXT_MUTED());
        g2.drawString("IronPulse", textX, h - Scale.dp(12));

        // Gear button
        drawGearButton(g2, w);
        g2.dispose();
    }

    private void drawGearButton(Graphics2D g2, int w) {
        int gx = w - gearW() - Scale.dp(4);
        int gy = (getHeight() - gearH()) / 2 + Scale.dp(2);
        int cx = gx + gearW() / 2, cy = gy + gearH() / 2;
        int cr = Scale.dp(10);

        // Button background — visible in both modes
        Color btnBg = gearHover
            ? (Theme.isDark() ? Theme.ACCENT_DARK : Theme.shift(Theme.ACCENT, 20))
            : (Theme.isDark() ? Theme.PANEL_MID()  : Theme.PANEL_MID());
        g2.setPaint(Theme.verticalGradient(btnBg, gearW(), gearH()));
        g2.fillRoundRect(gx, gy, gearW(), gearH(), cr, cr);
        g2.setColor(gearHover ? Theme.ACCENT_GLOW : Theme.BORDER());
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(gx, gy, gearW() - 1, gearH() - 1, cr, cr);

        Color iconCol = gearHover ? Color.WHITE : Theme.TEXT_MUTED();
        drawGearIcon(g2, cx, cy, iconCol);
    }

    private void drawGearIcon(Graphics2D g2, int cx, int cy, Color col) {
        int teeth   = 8;
        int outerR  = Scale.dp(9);
        int toothH  = Scale.dp(4);
        int innerR  = outerR - toothH + Scale.dp(1);

        GeneralPath gear = new GeneralPath();
        for (int i = 0; i < teeth; i++) {
            double a1 = 2*Math.PI*i/teeth - Math.PI/teeth*0.55;
            double a2 = 2*Math.PI*i/teeth - Math.PI/teeth*0.18;
            double a3 = 2*Math.PI*i/teeth + Math.PI/teeth*0.18;
            double a4 = 2*Math.PI*i/teeth + Math.PI/teeth*0.55;
            double na = 2*Math.PI*(i+1)/teeth - Math.PI/teeth*0.55;
            double ix1=cx+innerR*Math.cos(a1), iy1=cy+innerR*Math.sin(a1);
            double ox1=cx+outerR*Math.cos(a2), oy1=cy+outerR*Math.sin(a2);
            double ox2=cx+outerR*Math.cos(a3), oy2=cy+outerR*Math.sin(a3);
            double ix2=cx+innerR*Math.cos(a4), iy2=cy+innerR*Math.sin(a4);
            double nix=cx+innerR*Math.cos(na),  niy=cy+innerR*Math.sin(na);
            if (i==0) gear.moveTo(ix1,iy1); else gear.lineTo(ix1,iy1);
            gear.lineTo(ox1,oy1); gear.lineTo(ox2,oy2); gear.lineTo(ix2,iy2);
            gear.curveTo(cx+innerR*Math.cos(a4+0.12),cy+innerR*Math.sin(a4+0.12),
                         cx+innerR*Math.cos(na-0.12), cy+innerR*Math.sin(na-0.12), nix, niy);
        }
        gear.closePath();
        g2.setColor(col); g2.fill(gear);
        int hr = Scale.dp(4);
        g2.setColor(gearHover ? Theme.ACCENT_DARK : Theme.PANEL_DARK());
        g2.fillOval(cx-hr, cy-hr, hr*2, hr*2);
        g2.setColor(col); g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx-hr, cy-hr, hr*2, hr*2);
    }

    private void drawIcon(Graphics2D g2, int x, int y) {
        switch (type) {
            case "workout":
                g2.drawLine(x+3,y+15,x+30,y+15); g2.fillRoundRect(x,y+9,7,12,4,4); g2.fillRoundRect(x+26,y+9,7,12,4,4); break;
            case "macro":
                g2.drawOval(x+4,y+6,22,22); g2.draw(new Arc2D.Double(x+7,y+9,16,16,35,110,Arc2D.OPEN));
                g2.fillOval(x+14,y+14,4,4); g2.drawLine(x+17,y+16,x+25,y+8); break;
            case "pr":
                g2.drawLine(x+6,y+28,x+26,y+28); g2.drawLine(x+16,y+6,x+16,y+28);
                g2.drawLine(x+8,y+14,x+16,y+6); g2.drawLine(x+24,y+14,x+16,y+6); break;
            case "history":
                g2.fillRoundRect(x+3,y+20,5,8,3,3); g2.fillRoundRect(x+12,y+14,5,14,3,3);
                g2.fillRoundRect(x+21,y+8,5,20,3,3); g2.drawLine(x+2,y+28,x+28,y+28); break;
            case "body":
                g2.drawLine(x+4,y+28,x+4,y+6); g2.drawLine(x+4,y+28,x+28,y+28);
                g2.drawLine(x+4,y+20,x+11,y+16); g2.drawLine(x+11,y+16,x+18,y+20);
                g2.drawLine(x+18,y+20,x+28,y+8); g2.fillOval(x+25,y+5,5,5); break;
            case "assessment":
                g2.drawOval(x+5,y+6,22,22); g2.drawLine(x+16,y+10,x+16,y+17);
                g2.drawLine(x+16,y+17,x+22,y+13); g2.drawLine(x+8,y+30,x+24,y+30); break;
            case "cardio":
                g2.drawOval(x+6,y+6,20,20); g2.drawLine(x+16,y+6,x+16,y+2);
                g2.drawLine(x+16,y+16,x+22,y+11); g2.drawLine(x+3,y+26,x+29,y+26); break;
            case "settings":
                drawGearIcon(g2, x+16, y+16, Color.WHITE); break;
            default:
                g2.drawLine(x+4,y+28,x+4,y+6); g2.drawLine(x+4,y+28,x+28,y+28);
                g2.drawLine(x+4,y+21,x+12,y+16); g2.drawLine(x+12,y+16,x+20,y+20); g2.drawLine(x+20,y+20,x+28,y+9); break;
        }
    }
}
