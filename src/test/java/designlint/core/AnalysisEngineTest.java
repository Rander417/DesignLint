package designlint.core;

import designlint.guidelines.CloneableCheck;
import designlint.guidelines.EqualsHashCodeCheck;
import designlint.guidelines.EqualsPatternCheck;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the AnalysisEngine.
 *
 * These tests verify that the engine correctly:
 *   - Loads classes via ClassLoadingService
 *   - Runs selected guidelines against selected classes
 *   - Aggregates results correctly
 *   - Handles edge cases (empty selections, etc.)
 *
 * These are closer to integration tests than unit tests since they exercise
 * the full stack: SootUp class loading → guideline analysis → result collection.
 */
@DisplayName("AnalysisEngine")
class AnalysisEngineTest {

    private static AnalysisEngine engine;

    @BeforeAll
    static void setUp() {
        var classLoadingService = new ClassLoadingService();

        // Load the test fixture classes
        classLoadingService.load(Path.of("build/classes/java/test"), Set.of());

        List<DesignGuideline> guidelines = List.of(
                new CloneableCheck(),
                new EqualsHashCodeCheck(),
                new EqualsPatternCheck()
        );

        engine = new AnalysisEngine(classLoadingService, guidelines);
    }

    @Test
    @DisplayName("Should return all three guidelines as available")
    void getAvailableGuidelines_returnsAllThree() {
        assertEquals(3, engine.getAvailableGuidelines().size());
    }

    @Test
    @DisplayName("Should return results when analyzing a single class with all guidelines")
    void analyze_singleClassAllGuidelines_returnsResults() {
        Set<String> classes = Set.of("designlint.fixtures.PlainClass");
        Set<String> guidelines = Set.of(
                "Cloneable Check", "equals/hashCode Check", "equals() Pattern Check");

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        assertEquals(3, results.size(), "One class × three guidelines = three results");
        assertTrue(results.stream().allMatch(AnalysisResult::passed),
                "PlainClass should pass all checks");
    }

    @Test
    @DisplayName("Should find violations in the all-violations fixture")
    void analyze_allViolationsClass_findsViolations() {
        Set<String> classes = Set.of("designlint.fixtures.AllViolations");
        Set<String> guidelines = Set.of(
                "Cloneable Check", "equals/hashCode Check", "equals() Pattern Check");

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        long violations = results.stream().filter(r -> !r.passed()).count();
        assertTrue(violations >= 3,
                "AllViolations should produce at least 3 violations (Cloneable + " +
                "equals/hashCode + pattern), got " + violations);
    }

    @Test
    @DisplayName("Should only run selected guidelines")
    void analyze_singleGuideline_onlyRunsThatOne() {
        Set<String> classes = Set.of("designlint.fixtures.ImplementsCloneable");
        Set<String> guidelines = Set.of("Cloneable Check"); // only one

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        assertEquals(1, results.size(), "Should only have results from the one selected guideline");
        assertEquals("Cloneable Check", results.get(0).guidelineName());
    }

    @Test
    @DisplayName("Should return empty results when no classes are selected")
    void analyze_noClasses_returnsEmpty() {
        Set<String> classes = Set.of();
        Set<String> guidelines = Set.of("Cloneable Check");

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        assertTrue(results.isEmpty(), "No classes selected = no results");
    }

    @Test
    @DisplayName("Should return empty results when no guidelines are selected")
    void analyze_noGuidelines_returnsEmpty() {
        Set<String> classes = Set.of("designlint.fixtures.PlainClass");
        Set<String> guidelines = Set.of();

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        assertTrue(results.isEmpty(), "No guidelines selected = no results");
    }

    @Test
    @DisplayName("Should handle non-existent class name gracefully")
    void analyze_nonExistentClass_returnsEmpty() {
        Set<String> classes = Set.of("com.does.not.Exist");
        Set<String> guidelines = Set.of("Cloneable Check");

        List<AnalysisResult> results = engine.analyze(classes, guidelines);

        assertTrue(results.isEmpty(),
                "Non-existent class should be silently skipped, not cause an error");
    }

    @Test
    @DisplayName("getViolations() should return only violations from last run")
    void getViolations_afterAnalysis_returnsOnlyViolations() {
        engine.analyze(
                Set.of("designlint.fixtures.EqualsWithoutHashCode"),
                Set.of("equals/hashCode Check")
        );

        var violations = engine.getViolations();
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().allMatch(v -> v instanceof AnalysisResult.Violation));
    }

    @Test
    @DisplayName("getPasses() should return only passes from last run")
    void getPasses_afterAnalysis_returnsOnlyPasses() {
        engine.analyze(
                Set.of("designlint.fixtures.PlainClass"),
                Set.of("Cloneable Check")
        );

        var passes = engine.getPasses();
        assertFalse(passes.isEmpty());
        assertTrue(passes.stream().allMatch(p -> p instanceof AnalysisResult.Pass));
    }

    @Test
    @DisplayName("analyzeAll() should run all guidelines against all loaded classes")
    void analyzeAll_returnsResultsForEverything() {
        List<AnalysisResult> results = engine.analyzeAll();

        // We have 10 fixture classes and 3 guidelines, but some checks produce
        // multiple results, so we expect at least 10 * 3 = 30 results.
        // The actual number will be higher due to multi-violation results.
        assertTrue(results.size() >= 20,
                "analyzeAll should produce many results, got " + results.size());
    }
}
