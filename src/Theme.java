import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;

public final class Theme {
    public static final Color BG_DARK = new Color(15, 18, 22);
    public static final Color PANEL_DARK = new Color(26, 31, 37);
    public static final Color PANEL_MID = new Color(40, 48, 57);
    public static final Color CARD_BG = new Color(31, 38, 45);
    public static final Color CARD_BG_SELECTED = new Color(43, 55, 63);
    public static final Color ACCENT = new Color(22, 205, 137);
    public static final Color ACCENT_2 = new Color(255, 184, 77);
    public static final Color ACCENT_GLOW = new Color(22, 205, 137, 145);
    public static final Color TEXT = new Color(246, 249, 247);
    public static final Color TEXT_MUTED = new Color(151, 163, 170);
    public static final Color DANGER = new Color(220, 84, 84);
    public static final Color FIELD_BG = new Color(31, 35, 42);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 23);
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