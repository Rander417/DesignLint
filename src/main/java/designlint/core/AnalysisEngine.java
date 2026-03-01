package designlint.core;

import sootup.core.model.SootClass;
import sootup.java.core.views.JavaView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates analysis by running selected design guidelines against selected classes.
 *
 * This is the "brain" that connects the ClassLoadingService (which loads bytecodes)
 * with DesignGuideline implementations (which perform checks). The GUI interacts
 * primarily with this class.
 *
 * === ARCHITECTURE NOTE ===
 * The original design had analysis logic scattered across the check objects and the GUI.
 * This engine centralizes orchestration, making it easier to test (you can run the engine
 * without a GUI) and easier to extend (add batch mode, CLI mode, etc.).
 */
public class AnalysisEngine {

    private final ClassLoadingService classLoadingService;

    /** All available guidelines (the full menu of checks). */
    private final List<DesignGuideline> availableGuidelines;

    /** Results from the most recent analysis run. */
    private List<AnalysisResult> lastResults = List.of();

    public AnalysisEngine(ClassLoadingService classLoadingService,
                          List<DesignGuideline> guidelines) {
        this.classLoadingService = classLoadingService;
        this.availableGuidelines = List.copyOf(guidelines);
    }

    /** Get all available guidelines (for populating the UI checklist). */
    public List<DesignGuideline> getAvailableGuidelines() {
        return availableGuidelines;
    }

    /** Get the class loading service (for UI to trigger loading and display classes). */
    public ClassLoadingService getClassLoadingService() {
        return classLoadingService;
    }

    /**
     * Run selected guidelines against selected classes.
     *
     * @param classNames      fully qualified names of classes to analyze
     * @param guidelineNames  names of guidelines to run (matching DesignGuideline.name())
     * @return all analysis results (both passes and violations)
     */
    public List<AnalysisResult> analyze(Set<String> classNames, Set<String> guidelineNames) {
        if (!classLoadingService.isLoaded()) {
            return List.of();
        }

        JavaView view = classLoadingService.getView();

        // Filter to only the guidelines the user selected
        List<DesignGuideline> selectedGuidelines = availableGuidelines.stream()
                .filter(g -> guidelineNames.contains(g.name()))
                .collect(Collectors.toList());

        // Run each selected guideline against each selected class.
        // We build the results list explicitly here for clarity.
        List<AnalysisResult> results = new ArrayList<>();

        for (String className : classNames) {
            Optional<SootClass> maybeSootClass = classLoadingService.getClass(className);
            if (maybeSootClass.isPresent()) {
                SootClass sootClass = maybeSootClass.get();
                for (DesignGuideline guideline : selectedGuidelines) {
                    results.addAll(guideline.analyze(sootClass, view));
                }
            }
        }

        lastResults = results;
        return lastResults;
    }

    /** Get only violations from the last analysis run. */
    public List<AnalysisResult.Violation> getViolations() {
        return lastResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> (AnalysisResult.Violation) r)
                .collect(Collectors.toList());
    }

    /** Get only passes from the last analysis run. */
    public List<AnalysisResult.Pass> getPasses() {
        return lastResults.stream()
                .filter(r -> r instanceof AnalysisResult.Pass)
                .map(r -> (AnalysisResult.Pass) r)
                .collect(Collectors.toList());
    }

    /** Get all results from the last analysis run. */
    public List<AnalysisResult> getLastResults() {
        return lastResults;
    }

    /**
     * Convenience: run ALL guidelines against ALL loaded classes.
     * Useful for batch/CLI mode.
     */
    public List<AnalysisResult> analyzeAll() {
        Set<String> allClassNames = new LinkedHashSet<>();
        for (SootClass sc : classLoadingService.getAllClasses()) {
            allClassNames.add(sc.getType().getFullyQualifiedName());
        }

        Set<String> allGuidelineNames = new LinkedHashSet<>();
        for (DesignGuideline g : availableGuidelines) {
            allGuidelineNames.add(g.name());
        }

        return analyze(allClassNames, allGuidelineNames);
    }
}
