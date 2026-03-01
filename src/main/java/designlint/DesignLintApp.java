package designlint;

import designlint.core.AnalysisEngine;
import designlint.core.ClassLoadingService;
import designlint.core.DesignGuideline;
import designlint.guidelines.CloneableCheck;
import designlint.guidelines.EqualsHashCodeCheck;
import designlint.guidelines.EqualsPatternCheck;
import designlint.ui.MainWindow;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

/**
 * DesignLint — A static analysis tool for Java design guidelines.
 *
 * This is the entry point. It wires together:
 *   - The ClassLoadingService (SootUp integration)
 *   - The available DesignGuideline implementations
 *   - The AnalysisEngine (orchestration)
 *   - The MainWindow (JavaFX UI)
 *
 * === JAVAFX APPLICATION LIFECYCLE ===
 * JavaFX apps extend Application and override start(Stage).
 * The Stage is the main window — JavaFX creates it for you.
 * The launch() method (called from main) sets up the JavaFX runtime,
 * creates a Stage, and calls start().
 *
 * === ADDING NEW GUIDELINES ===
 * To add a new design check:
 *   1. Create a new class implementing DesignGuideline
 *   2. Add it to the list in createGuidelines() below
 *   That's it — the engine and UI will pick it up automatically.
 *
 * For a future enhancement, we could use Java's ServiceLoader to discover
 * guidelines at runtime from the classpath, eliminating even step 2.
 */
public class DesignLintApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Create the service that handles SootUp class loading
        var classLoadingService = new ClassLoadingService();

        // 2. Create all available design guidelines
        List<DesignGuideline> guidelines = createGuidelines();

        // 3. Create the analysis engine that orchestrates everything
        var engine = new AnalysisEngine(classLoadingService, guidelines);

        // 4. Build and show the UI
        new MainWindow(primaryStage, engine);
    }

    /**
     * Register all available design guidelines.
     * This is the single place where new checks are added to the system.
     */
    private List<DesignGuideline> createGuidelines() {
        return List.of(
                new CloneableCheck(),
                new EqualsHashCodeCheck(),
                new EqualsPatternCheck()
        );
    }

    /**
     * Standard Java entry point.
     * launch() is a static method from Application that bootstraps JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
