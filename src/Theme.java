import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;

public final class Theme {
    public static final Color BG_DARK = new Color(24, 27, 32);
    public static final Color PANEL_DARK = new Color(35, 39, 47);
    public static final Color PANEL_MID = new Color(48, 55, 66);
    public static final Color CARD_BG = new Color(43, 49, 58);
    public static final Color CARD_BG_SELECTED = new Color(56, 66, 82);
    public static final Color ACCENT = new Color(42, 185, 132);
    public static final Color ACCENT_2 = new Color(248, 174, 75);
    public static final Color ACCENT_GLOW = new Color(76, 224, 170, 150);
    public static final Color TEXT = new Color(244, 247, 248);
    public static final Color TEXT_MUTED = new Color(164, 175, 184);
    public static final Color DANGER = new Color(220, 84, 84);
    public static final Color FIELD_BG = new Color(31, 35, 42);

    public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 22);
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    public static final Font BODY_BOLD = new Font("Arial", Font.BOLD, 14);
    public static final Font BODY = new Font("Arial", Font.PLAIN, 13);
    public static final Font SMALL_BOLD = new Font("Arial", Font.BOLD, 12);
    public static final Font SMALL = new Font("Arial", Font.PLAIN, 12);

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