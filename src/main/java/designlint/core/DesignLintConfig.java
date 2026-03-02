package designlint.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration data model for DesignLint.
 *
 * This is a plain mutable POJO — Gson serializes its fields directly to/from JSON.
 * No annotations or special Gson config needed; Gson reads/writes all non-transient fields.
 *
 * === CONFIG LAYERING ===
 *
 * Two config files can exist simultaneously:
 *   1. Global:  ~/.designlint/config.json   (user-level defaults)
 *   2. Project: designlint.json             (next to analyzed classes, overrides global)
 *
 * Project config is intentionally sparse — it only specifies what differs from global.
 * The ConfigService merges them: project values win where specified, global fills the gaps.
 *
 * === WHAT GETS SAVED ===
 *
 * Settings dialog (intentional defaults):
 *   - guidelines:     which checks are enabled by default
 *   - filters:        which severity levels to show by default
 *   - viewMode:       "tree" or "table"
 *   - sortBySeverity: default sort preference
 *
 * Auto-saved on close (session state):
 *   - lastClasspath:  last directory loaded (quality-of-life)
 *   - window:         size and position (restored on next launch)
 *
 * === JSON EXAMPLE ===
 *
 * {
 *   "guidelines": {
 *     "Cloneable Check": true,
 *     "equals/hashCode Check": true,
 *     "toString Check": false
 *   },
 *   "filters": {
 *     "showErrors": true,
 *     "showWarnings": true,
 *     "showAdvisories": true,
 *     "showPasses": false
 *   },
 *   "viewMode": "tree",
 *   "sortBySeverity": false,
 *   "lastClasspath": "C:/projects/myapp/build/classes",
 *   "window": {
 *     "width": 1050.0,
 *     "height": 720.0,
 *     "x": 100.0,
 *     "y": 100.0
 *   }
 * }
 */
public class DesignLintConfig {

    // --- Guideline enable/disable ---
    // Key = guideline name (DesignGuideline.name()), value = enabled
    // If a guideline isn't in the map, it defaults to enabled.
    private Map<String, Boolean> guidelines = new LinkedHashMap<>();

    // --- Filter defaults ---
    private FilterConfig filters = new FilterConfig();

    // --- View preferences ---
    private String viewMode = "tree";        // "tree" or "table"
    private boolean sortBySeverity = false;

    // --- Session state ---
    private String lastClasspath = null;
    private WindowConfig window = new WindowConfig();

    // --- Nested config objects ---

    public static class FilterConfig {
        private boolean showErrors = true;
        private boolean showWarnings = true;
        private boolean showAdvisories = true;
        private boolean showPasses = true;

        public boolean isShowErrors()     { return showErrors; }
        public boolean isShowWarnings()   { return showWarnings; }
        public boolean isShowAdvisories() { return showAdvisories; }
        public boolean isShowPasses()     { return showPasses; }

        public void setShowErrors(boolean v)     { showErrors = v; }
        public void setShowWarnings(boolean v)   { showWarnings = v; }
        public void setShowAdvisories(boolean v) { showAdvisories = v; }
        public void setShowPasses(boolean v)     { showPasses = v; }
    }

    public static class WindowConfig {
        private double width = 1050;
        private double height = 720;
        private double x = -1;   // -1 means "let the OS decide" (not yet saved)
        private double y = -1;

        public double getWidth()  { return width; }
        public double getHeight() { return height; }
        public double getX()      { return x; }
        public double getY()      { return y; }

        public void setWidth(double v)  { width = v; }
        public void setHeight(double v) { height = v; }
        public void setX(double v)      { x = v; }
        public void setY(double v)      { y = v; }
    }

    // --- Accessors ---

    public Map<String, Boolean> getGuidelines() { return guidelines; }
    public void setGuidelines(Map<String, Boolean> g) { guidelines = g; }

    public FilterConfig getFilters() { return filters; }
    public void setFilters(FilterConfig f) { filters = f; }

    public String getViewMode() { return viewMode; }
    public void setViewMode(String v) { viewMode = v; }

    public boolean isSortBySeverity() { return sortBySeverity; }
    public void setSortBySeverity(boolean v) { sortBySeverity = v; }

    public String getLastClasspath() { return lastClasspath; }
    public void setLastClasspath(String v) { lastClasspath = v; }

    public WindowConfig getWindow() { return window; }
    public void setWindow(WindowConfig w) { window = w; }

    // --- Helper ---

    /**
     * Check if a guideline is enabled in this config.
     * Guidelines not present in the map default to enabled.
     */
    public boolean isGuidelineEnabled(String guidelineName) {
        return guidelines.getOrDefault(guidelineName, true);
    }
}
