package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ThrowsGenericCheck guideline.
 *
 * Covers:
 *   - Method declaring throws Exception → violation
 *   - Method declaring throws IOException (specific) → pass
 *   - Class with no throws declarations → pass
 */
@DisplayName("ThrowsGenericCheck")
class ThrowsGenericCheckTest {

    private static TestHelper helper;
    private static ThrowsGenericCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new ThrowsGenericCheck();
    }

    @Test
    @DisplayName("Should flag a method that declares throws Exception")
    void analyze_throwsException_returnsViolation() {
        var sootClass = helper.fixture("ThrowsGenericException");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean hasViolation = results.stream()
                .anyMatch(r -> r instanceof AnalysisResult.Violation);
        assertTrue(hasViolation, "Declaring throws Exception should produce a violation");

        var violation = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> (AnalysisResult.Violation) r)
                .findFirst().orElseThrow();
        assertTrue(violation.message().contains("too broad"),
                "Violation should explain the throws clause is too broad");
    }

    @Test
    @DisplayName("Should pass a method that declares a specific exception type")
    void analyze_throwsSpecific_returnsPass() {
        var sootClass = helper.fixture("ThrowsSpecificException");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Specific throws declaration should pass");
    }

    @Test
    @DisplayName("Should pass a class with no throws declarations")
    void analyze_noThrows_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Generic throws Check'")
    void name_returnsExpectedName() {
        assertEquals("Generic throws Check", check.name());
    }
}
