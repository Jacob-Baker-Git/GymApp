import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

/**
 * Provides screen-relative sizing so the UI adapts to any window/screen size.
 * Call Scale.init() once at startup after the window is visible.
 * All sizing should go through Scale.dp() and Scale.sp() rather than hardcoded pixels.
 */
public final class Scale {

    private static float density = 1.0f;
    private static int   baseW   = 480;
    private static int   baseH   = 800;

    private Scale() {}

    /** Call once the window is shown or resized. */
    public static void init(int windowW, int windowH) {
        float sw = windowW / (float) baseW;
        float sh = windowH / (float) baseH;
        // Use the smaller axis to avoid clipping
        density = Math.min(sw, sh);
        // Clamp so tiny windows and huge monitors stay reasonable
        density = Math.max(0.55f, Math.min(density, 2.4f));
    }

    /** Density-independent pixels → screen pixels. */
    public static int dp(int dp)   { return Math.max(1, Math.round(dp * density)); }
    public static int dp(float dp) { return Math.max(1, Math.round(dp * density)); }

    /** Scale a font size. */
    public static Font font(Font base) {
        int size = Math.max(8, Math.round(base.getSize() * density));
        return base.deriveFont((float) size);
    }

    /** Scale a Dimension. */
    public static Dimension dim(int w, int h) {
        return new Dimension(dp(w), dp(h));
    }

    public static float getDensity() { return density; }
}
