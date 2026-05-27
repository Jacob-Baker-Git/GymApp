import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;

public final class Theme {

    private static boolean dark = true;

    public static void setDark(boolean isDark) { dark = isDark; }
    public static boolean isDark()             { return dark; }

    // ── Backgrounds ──────────────────────────────────────────────────────────
    public static Color BG_DARK()          { return dark ? new Color(10,  12,  18)       : new Color(238, 242, 248); }
    public static Color BG_TOP()           { return dark ? new Color(14,  18,  26)        : new Color(245, 248, 252); }
    public static Color PANEL_DARK()       { return dark ? new Color(20,  26,  36)        : new Color(255, 255, 255); }
    public static Color PANEL_MID()        { return dark ? new Color(32,  40,  54)        : new Color(220, 228, 238); }
    public static Color CARD_BG()          { return dark ? new Color(24,  30,  44)        : new Color(255, 255, 255); }
    public static Color CARD_BG_SELECTED() { return dark ? new Color(28,  58,  76)        : new Color(210, 240, 228); }
    public static Color FIELD_BG()         { return dark ? new Color(16,  20,  30)        : new Color(235, 240, 248); }
    public static Color BORDER()           { return dark ? new Color(48,  62,  84, 150)   : new Color(190, 204, 222, 180); }

    // ── Text ─────────────────────────────────────────────────────────────────
    public static Color TEXT()             { return dark ? new Color(238, 244, 252)       : new Color(10,  18,  30); }
    public static Color TEXT_MUTED()       { return dark ? new Color(114, 132, 158)       : new Color(80,  100, 124); }
    public static Color TEXT_DIM()         { return dark ? new Color(52,  66,  88)        : new Color(160, 176, 196); }

    // ── Accents ───────────────────────────────────────────────────────────────
    public static final Color ACCENT       = new Color(34,  200, 130);
    public static final Color ACCENT_DARK  = new Color(16,  146, 90);
    public static final Color ACCENT_GLOW  = new Color(34,  224, 150, 130);
    public static final Color ACCENT_2     = new Color(255, 188, 56);
    public static final Color DANGER       = new Color(232, 68,  68);

    // ── Calendar ─────────────────────────────────────────────────────────────
    public static Color CAL_TODAY()   { return dark ? new Color(20,  78,  148)  : new Color(48,  116, 210); }
    public static Color CAL_WORKOUT() { return dark ? new Color(154, 68,  16)   : new Color(210, 92,  18); }
    public static Color CAL_REST()    { return dark ? new Color(18,  96,  48)   : new Color(28,  148, 72); }
    public static Color CAL_EMPTY()   { return dark ? new Color(20,  26,  36)   : new Color(236, 240, 248); }

    // ── Fonts (base sizes — Scale.font() wraps these for adaptive sizing) ────
    public static Font titleFont()  { return Scale.font(new Font("Segoe UI Semibold", Font.BOLD,  22)); }
    public static Font headerFont() { return Scale.font(new Font("Segoe UI",          Font.BOLD,  17)); }
    public static Font bodyBold()   { return Scale.font(new Font("Segoe UI",          Font.BOLD,  14)); }
    public static Font body()       { return Scale.font(new Font("Segoe UI",          Font.PLAIN, 13)); }
    public static Font smallBold()  { return Scale.font(new Font("Segoe UI",          Font.BOLD,  11)); }
    public static Font small()      { return Scale.font(new Font("Segoe UI",          Font.PLAIN, 11)); }
    public static Font tiny()       { return Scale.font(new Font("Segoe UI",          Font.PLAIN, 10)); }

    // Legacy static constants — delegate to methods so existing call sites compile
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
        return new GradientPaint(0, 0, shift(base, dark ? 12 : 5), 0, h, shift(base, dark ? -10 : -7));
    }

    public static Color shift(Color c, int amt) {
        return new Color(clamp(c.getRed()+amt), clamp(c.getGreen()+amt), clamp(c.getBlue()+amt), c.getAlpha());
    }

    /** Blend two colours. t=0 → a, t=1 → b. */
    public static Color blend(Color a, Color b, float t) {
        float s = 1 - t;
        return new Color(
            clamp(Math.round(a.getRed()  *s + b.getRed()  *t)),
            clamp(Math.round(a.getGreen()*s + b.getGreen()*t)),
            clamp(Math.round(a.getBlue() *s + b.getBlue() *t)),
            clamp(Math.round(a.getAlpha()*s + b.getAlpha()*t)));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
