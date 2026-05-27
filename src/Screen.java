/**
 * Implemented by any panel that needs a callback when the user navigates to it,
 * allowing lazy refresh of dynamic content.
 */
public interface Screen {
    void onNavigateTo();
}
