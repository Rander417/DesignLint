package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EmptyCatchCheck guideline.
 *
 * This exercises body-level Jimple analysis — we're looking inside method
 * implementations for exception handler patterns, not just at signatures.
 */
@DisplayName("EmptyCatchCheck")
class EmptyCatchCheckTest {

    private static TestHelper helper;
    private static EmptyCatchCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new EmptyCatchCheck();
    }

    @Test
    @DisplayName("Should flag a method with an empty catch block")
    void analyze_emptyCatch_returnsViolation() {
        var sootClass = helper.fixture("EmptyCatchBlock");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean hasViolation = results.stream()
                .anyMatch(r -> r instanceof AnalysisResult.Violation);
        assertTrue(hasViolation, "Empty catch block should produce a violation");

        var violation = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> (AnalysisResult.Violation) r)
                .findFirst().orElseThrow();
        assertTrue(violation.message().contains("empty catch"),
                "Violation should mention empty catch block");
    }

    @Test
    @DisplayName("Should pass a method with a non-empty catch block")
    void analyze_nonEmptyCatch_returnsPass() {
        var sootClass = helper.fixture("NonEmptyCatchBlock");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Non-empty catch blocks should not produce violations");
    }

    @Test
    @DisplayName("Should pass a class with no try-catch at all")
    void analyze_noCatch_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Empty Catch Block Check'")
    void name_returnsExpectedName() {
        assertEquals("Empty Catch Block Check", check.name());
    }
}
