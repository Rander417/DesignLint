package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FunctionalInterfaceCheck guideline.
 *
 * Covers:
 *   - SAM interface WITHOUT @FunctionalInterface → violation
 *   - SAM interface WITH @FunctionalInterface → pass
 *   - Interface with multiple methods (not a SAM) → pass
 *   - Empty interface (no methods) → pass (not a SAM candidate)
 *   - Regular class → pass
 */
@DisplayName("FunctionalInterfaceCheck")
class FunctionalInterfaceCheckTest {

    private static TestHelper helper;
    private static FunctionalInterfaceCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new FunctionalInterfaceCheck();
    }

    @Test
    @DisplayName("Should flag SAM interface missing @FunctionalInterface")
    void analyze_samWithoutAnnotation_returnsViolation() {
        var sootClass = helper.fixture("NonEmptyInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("@FunctionalInterface"),
                "Violation should mention the missing annotation");
    }

    @Test
    @DisplayName("Should pass SAM interface that has @FunctionalInterface")
    void analyze_samWithAnnotation_returnsPass() {
        var sootClass = helper.fixture("AnnotatedFunctionalInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass interface with multiple abstract methods")
    void analyze_multiMethodInterface_returnsPass() {
        var sootClass = helper.fixture("MultiMethodInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass empty interface (0 methods, not a SAM candidate)")
    void analyze_emptyInterface_returnsPass() {
        var sootClass = helper.fixture("EmptyMarkerInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass a regular class")
    void analyze_regularClass_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be '@FunctionalInterface Check'")
    void name_returnsExpectedName() {
        assertEquals("@FunctionalInterface Check", check.name());
    }
}
