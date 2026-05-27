import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;

public final class Theme {

    private static boolean dark = true;

    public static void setDark(boolean isDark) { dark = isDark; }
    public static boolean isDark()             { return dark; }

    // Backgrounds
    public static Color BG_DARK()          { return dark ? new Color(10, 12, 16)       : new Color(232, 236, 240); }
    public static Color BG_TOP()           { return dark ? new Color(14, 18, 24)        : new Color(240, 244, 248); }
    public static Color PANEL_DARK()       { return dark ? new Color(20, 25, 32)        : new Color(252, 253, 255); }
    public static Color PANEL_MID()        { return dark ? new Color(32, 40, 52)        : new Color(212, 220, 230); }
    public static Color CARD_BG()          { return dark ? new Color(24, 30, 42)        : new Color(255, 255, 255); }
    public static Color CARD_BG_SELECTED() { return dark ? new Color(30, 55, 70)        : new Color(200, 235, 220); }
    public static Color FIELD_BG()         { return dark ? new Color(18, 22, 30)        : new Color(228, 234, 240); }
    public static Color BORDER()           { return dark ? new Color(50, 62, 80, 140)   : new Color(180, 192, 208, 160); }
    public static Color TEXT()             { return dark ? new Color(240, 244, 248)     : new Color(12, 18, 28); }
    public static Color TEXT_MUTED()       { return dark ? new Color(120, 136, 158)     : new Color(90, 108, 128); }
    public static Color TEXT_DIM()         { return dark ? new Color(60, 74, 92)        : new Color(150, 165, 180); }

    // Accents - same in both modes
    public static final Color ACCENT       = new Color(32,  196, 128);
    public static final Color ACCENT_DARK  = new Color(14,  140, 88);
    public static final Color ACCENT_GLOW  = new Color(32,  220, 148, 140);
    public static final Color ACCENT_2     = new Color(255, 185, 55);
    public static final Color DANGER       = new Color(230, 72,  72);

    // Calendar colours
    public static Color CAL_TODAY()        { return dark ? new Color(22, 80, 145)       : new Color(40, 110, 200); }
    public static Color CAL_WORKOUT()      { return dark ? new Color(160, 72, 20)       : new Color(200, 90, 20); }
    public static Color CAL_REST()         { return dark ? new Color(20, 100, 50)       : new Color(30, 140, 70); }
    public static Color CAL_EMPTY()        { return dark ? new Color(20, 25, 32)        : new Color(230, 235, 242); }

    // Fonts
    public static final Font TITLE_FONT  = new Font("Segoe UI Semibold", Font.BOLD,  22);
    public static final Font HEADER_FONT = new Font("Segoe UI",          Font.BOLD,  17);
    public static final Font BODY_BOLD   = new Font("Segoe UI",          Font.BOLD,  14);
    public static final Font BODY        = new Font("Segoe UI",          Font.PLAIN, 13);
    public static final Font SMALL_BOLD  = new Font("Segoe UI",          Font.BOLD,  11);
    public static final Font SMALL       = new Font("Segoe UI",          Font.PLAIN, 11);
    public static final Font TINY        = new Font("Segoe UI",          Font.PLAIN, 10);

    private Theme() {}

    public static Paint verticalGradient(Color base, int width, int height) {
        int h = Math.max(1, height);
        return new GradientPaint(0, 0, shift(base, dark ? 14 : 6), 0, h, shift(base, dark ? -12 : -8));
    }

    public static Color shift(Color c, int amt) {
        return new Color(clamp(c.getRed()+amt), clamp(c.getGreen()+amt), clamp(c.getBlue()+amt), c.getAlpha());
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
