package designlint;

import designlint.core.AnalysisEngine;
import designlint.core.ClassLoadingService;
import designlint.core.ConfigService;
import designlint.core.DesignGuideline;
import designlint.guidelines.CatchAllCheck;
import designlint.guidelines.CloneableCheck;
import designlint.guidelines.ConcreteReturnTypeCheck;
import designlint.guidelines.CovariantEqualsCheck;
import designlint.guidelines.CompareToEqualsCheck;
import designlint.guidelines.EmptyCatchCheck;
import designlint.guidelines.EmptyInterfaceCheck;
import designlint.guidelines.EqualsHashCodeCheck;
import designlint.guidelines.EqualsPatternCheck;
import designlint.guidelines.FinalizeCheck;
import designlint.guidelines.FunctionalInterfaceCheck;
import designlint.guidelines.MutablePublicFieldCheck;
import designlint.guidelines.MutableStaticCheck;
import designlint.guidelines.OptionalMisuseCheck;
import designlint.guidelines.SerializableCheck;
import designlint.guidelines.ThrowsGenericCheck;
import designlint.guidelines.ToStringCheck;
import designlint.guidelines.UtilityClassConstructorCheck;
import designlint.ui.MainWindow;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

/**
 * DesignLint — A static analysis tool for Java design guidelines.
 *
 * This is the entry point. It wires together:
 *   - The ConfigService (persistent configuration)
 *   - The ClassLoadingService (SootUp integration)
 *   - The available DesignGuideline implementations
 *   - The AnalysisEngine (orchestration)
 *   - The MainWindow (JavaFX UI)
 *
 * === STARTUP SEQUENCE ===
 *   1. Load global config from ~/.designlint/config.json
 *   2. Create the class loading service and analysis engine
 *   3. Build the UI and apply saved configuration
 *   4. On close, session state auto-saves back to global config
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
        // 1. Load saved configuration (global defaults)
        var configService = new ConfigService();
        configService.load(null);  // no project dir yet — just global config

        // 2. Create the service that handles SootUp class loading
        var classLoadingService = new ClassLoadingService();

        // 3. Create all available design guidelines
        List<DesignGuideline> guidelines = createGuidelines();

        // 4. Create the analysis engine that orchestrates everything
        var engine = new AnalysisEngine(classLoadingService, guidelines);

        // 5. Build and show the UI (config is applied inside MainWindow constructor)
        new MainWindow(primaryStage, engine, configService);
    }

    /**
     * Register all available design guidelines.
     * This is the single place where new checks are added to the system.
     */
    private List<DesignGuideline> createGuidelines() {
        return List.of(
                // Original three checks (from the 2002 requirements)
                new CloneableCheck(),
                new EqualsHashCodeCheck(),
                new EqualsPatternCheck(),
                // Classic Java pitfalls
                new SerializableCheck(),
                new CovariantEqualsCheck(),
                new FinalizeCheck(),
                new CompareToEqualsCheck(),
                new ToStringCheck(),
                // Exception handling
                new EmptyCatchCheck(),
                new CatchAllCheck(),
                new ThrowsGenericCheck(),
                // Interface & type design
                new EmptyInterfaceCheck(),
                new FunctionalInterfaceCheck(),
                new ConcreteReturnTypeCheck(),
                // Modern Java best practices
                new MutableStaticCheck(),
                new OptionalMisuseCheck(),
                new MutablePublicFieldCheck(),
                new UtilityClassConstructorCheck()
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
