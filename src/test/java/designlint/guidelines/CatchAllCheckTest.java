package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CatchAllCheck guideline.
 *
 * Covers:
 *   - Method catching Exception broadly → violation
 *   - Method catching specific exception → pass
 *   - Class with no try-catch → pass
 */
@DisplayName("CatchAllCheck")
class CatchAllCheckTest {

    private static TestHelper helper;
    private static CatchAllCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new CatchAllCheck();
    }

    @Test
    @DisplayName("Should flag a method that catches Exception broadly")
    void analyze_catchesException_returnsViolation() {
        var sootClass = helper.fixture("CatchesGenericException");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean hasViolation = results.stream()
                .anyMatch(r -> r instanceof AnalysisResult.Violation);
        assertTrue(hasViolation, "Catching Exception should produce a violation");

        var violation = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> (AnalysisResult.Violation) r)
                .findFirst().orElseThrow();
        assertTrue(violation.message().contains("too broad"),
                "Violation should explain that the catch is too broad");
    }

    @Test
    @DisplayName("Should pass a method that catches specific exceptions")
    void analyze_catchesSpecific_returnsPass() {
        var sootClass = helper.fixture("CatchesSpecificException");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Catching specific exceptions should pass");
    }

    @Test
    @DisplayName("Should pass a class with no try-catch blocks")
    void analyze_noCatch_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Catch-All Exception Check'")
    void name_returnsExpectedName() {
        assertEquals("Catch-All Exception Check", check.name());
    }
}
