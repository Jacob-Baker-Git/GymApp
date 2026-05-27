import java.util.prefs.Preferences;

/**
 * Persists user preferences (dark/light mode, height) using java.util.prefs.
 * All gym data is saved separately via DataStore.
 */
public final class AppSettings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);

    private static final String KEY_DARK_MODE = "darkMode";
    private static final String KEY_HEIGHT_CM = "heightCm";

    private AppSettings() {}

    public static boolean isDarkMode() {
        return PREFS.getBoolean(KEY_DARK_MODE, true);
    }

    public static void setDarkMode(boolean dark) {
        PREFS.putBoolean(KEY_DARK_MODE, dark);
    }

    public static double getHeightCm() {
        return PREFS.getDouble(KEY_HEIGHT_CM, 0.0);
    }

    public static void setHeightCm(double cm) {
        PREFS.putDouble(KEY_HEIGHT_CM, cm);
    }
}
