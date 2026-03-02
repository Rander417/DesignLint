package designlint.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Manages loading, saving, and merging DesignLint configuration.
 *
 * === CONFIG LAYERING ===
 *
 * Two config files form a hierarchy:
 *
 *   1. Global config:  ~/.designlint/config.json
 *      - User-level defaults that apply everywhere
 *      - Created automatically on first save
 *      - This is what the Settings dialog edits
 *
 *   2. Project config: {classpath-dir}/designlint.json
 *      - Per-project overrides, lives next to the analyzed classes
 *      - Intentionally sparse — only specifies what differs from global
 *      - Loaded automatically when classes are loaded from that directory
 *
 * Merge strategy: start with defaults → apply global → apply project.
 * Each layer only overwrites values it explicitly specifies.
 *
 * === SESSION STATE ===
 *
 * Session state (window size/position, last classpath, current filter/view settings)
 * auto-saves to the global config on window close. This way your workspace is
 * restored next time you launch DesignLint.
 *
 * === WHY GSON? ===
 *
 * Gson is a single JAR with zero transitive dependencies — perfect for a tool like this.
 * It reads/writes POJO fields directly (no annotations needed). The pretty-printing
 * makes the config files human-editable, which matters for the per-project config
 * that someone might want to check into version control.
 */
public class ConfigService {

    /** Global config location: ~/.designlint/config.json */
    private static final String GLOBAL_DIR_NAME = ".designlint";
    private static final String GLOBAL_FILE_NAME = "config.json";

    /** Project config file name (lives next to analyzed classes). */
    private static final String PROJECT_FILE_NAME = "designlint.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** The currently active (merged) configuration. */
    private DesignLintConfig activeConfig = new DesignLintConfig();

    /**
     * The directory from which project config was loaded (if any).
     * Used to know where to save project-level changes.
     */
    private Path projectConfigDir = null;

    /**
     * Load configuration. Merges global defaults with any project-level overrides.
     *
     * Call this at startup (global only) and again when classes are loaded
     * from a directory (to pick up project config).
     *
     * @param classesDir optional directory where classes were loaded from;
     *                   if non-null, looks for designlint.json there
     */
    public void load(Path classesDir) {
        // Start fresh from defaults
        DesignLintConfig config = new DesignLintConfig();

        // Layer 1: Global config
        Path globalPath = getGlobalConfigPath();
        if (Files.exists(globalPath)) {
            DesignLintConfig global = readConfig(globalPath);
            if (global != null) {
                mergeInto(config, global);
            }
        }

        // Layer 2: Project config (if a classes directory was provided)
        if (classesDir != null) {
            projectConfigDir = classesDir;
            Path projectPath = classesDir.resolve(PROJECT_FILE_NAME);
            if (Files.exists(projectPath)) {
                DesignLintConfig project = readConfig(projectPath);
                if (project != null) {
                    mergeInto(config, project);
                }
            }
        }

        activeConfig = config;
    }

    /**
     * Save the active configuration to the global config file.
     * Called on window close to persist session state.
     */
    public void saveGlobal() {
        Path globalPath = getGlobalConfigPath();
        writeConfig(globalPath, activeConfig);
    }

    /**
     * Save a project-level config to the directory where classes were loaded.
     * Only saves guideline and analysis settings (not window state).
     *
     * @return true if saved successfully, false if no project directory is set
     */
    public boolean saveProject() {
        if (projectConfigDir == null) return false;

        // Project config is sparse — only save analysis-related settings,
        // not window geometry or session state
        DesignLintConfig projectConfig = new DesignLintConfig();
        projectConfig.setGuidelines(activeConfig.getGuidelines());
        projectConfig.setFilters(activeConfig.getFilters());
        projectConfig.setViewMode(activeConfig.getViewMode());
        projectConfig.setSortBySeverity(activeConfig.isSortBySeverity());

        Path projectPath = projectConfigDir.resolve(PROJECT_FILE_NAME);
        writeConfig(projectPath, projectConfig);
        return true;
    }

    /** Get the active (merged) configuration. */
    public DesignLintConfig getConfig() {
        return activeConfig;
    }

    /** Whether a project config directory has been set. */
    public boolean hasProjectDir() {
        return projectConfigDir != null;
    }

    /** Get the project config directory, if set. */
    public Path getProjectConfigDir() {
        return projectConfigDir;
    }

    /** Path to the global config file. */
    public Path getGlobalConfigPath() {
        return Path.of(System.getProperty("user.home"), GLOBAL_DIR_NAME, GLOBAL_FILE_NAME);
    }

    // ==================================================================================
    // Merge logic
    // ==================================================================================

    /**
     * Merge source values into target. Source values overwrite target values
     * where specified. This is the core of the layering system.
     *
     * For maps (like guidelines), each key in source overwrites that key in target.
     * Keys not present in source are left unchanged in target.
     */
    private void mergeInto(DesignLintConfig target, DesignLintConfig source) {
        // Guidelines: per-key merge
        if (source.getGuidelines() != null && !source.getGuidelines().isEmpty()) {
            for (Map.Entry<String, Boolean> entry : source.getGuidelines().entrySet()) {
                target.getGuidelines().put(entry.getKey(), entry.getValue());
            }
        }

        // Filters: overwrite if source has non-default values
        if (source.getFilters() != null) {
            target.setFilters(source.getFilters());
        }

        // Scalars: overwrite
        target.setViewMode(source.getViewMode());
        target.setSortBySeverity(source.isSortBySeverity());

        // Session state: only overwrite if source has values
        if (source.getLastClasspath() != null) {
            target.setLastClasspath(source.getLastClasspath());
        }
        if (source.getWindow() != null && source.getWindow().getX() >= 0) {
            target.setWindow(source.getWindow());
        }
    }

    // ==================================================================================
    // File I/O
    // ==================================================================================

    /** Read a config file, returning null on any error. */
    private DesignLintConfig readConfig(Path path) {
        try {
            String json = Files.readString(path);
            return gson.fromJson(json, DesignLintConfig.class);
        } catch (Exception e) {
            System.err.println("Warning: Could not read config from " + path + ": " + e.getMessage());
            return null;
        }
    }

    /** Write a config to a file, creating parent directories as needed. */
    private void writeConfig(Path path, DesignLintConfig config) {
        try {
            Files.createDirectories(path.getParent());
            String json = gson.toJson(config);
            Files.writeString(path, json);
        } catch (IOException e) {
            System.err.println("Warning: Could not save config to " + path + ": " + e.getMessage());
        }
    }
}
