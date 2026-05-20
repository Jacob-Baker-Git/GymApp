import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;

public final class Theme {
    public static final Color BG_DARK = new Color(11, 14, 18);
    public static final Color BG_TOP = new Color(21, 29, 34);
    public static final Color PANEL_DARK = new Color(24, 30, 36);
    public static final Color PANEL_MID = new Color(42, 52, 60);
    public static final Color CARD_BG = new Color(30, 38, 45);
    public static final Color CARD_BG_SELECTED = new Color(44, 61, 66);
    public static final Color BORDER = new Color(72, 86, 94, 135);
    public static final Color ACCENT = new Color(24, 214, 145);
    public static final Color ACCENT_DARK = new Color(12, 142, 107);
    public static final Color ACCENT_2 = new Color(255, 177, 68);
    public static final Color ACCENT_GLOW = new Color(24, 214, 145, 150);
    public static final Color TEXT = new Color(246, 249, 247);
    public static final Color TEXT_MUTED = new Color(151, 163, 170);
    public static final Color DANGER = new Color(220, 84, 84);
    public static final Color FIELD_BG = new Color(31, 35, 42);

    public static final Font TITLE_FONT = new Font("Segoe UI Semibold", Font.BOLD, 24);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private Theme() {
    }

    public static Paint verticalGradient(Color base, int width, int height) {
        int h = Math.max(1, height);
        Color top = shift(base, 18);
        Color bottom = shift(base, -16);
        return new GradientPaint(0, 0, top, 0, h, bottom);
    }

    public static Color shift(Color color, int amount) {
        return new Color(
                clamp(color.getRed() + amount),
                clamp(color.getGreen() + amount),
                clamp(color.getBlue() + amount),
                color.getAlpha()
        );
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}