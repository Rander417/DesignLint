package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CompareToEqualsCheck guideline.
 *
 * Covers:
 *   - Comparable WITH equals → pass
 *   - Comparable WITHOUT equals → violation
 *   - Not Comparable → pass
 */
@DisplayName("CompareToEqualsCheck")
class CompareToEqualsCheckTest {

    private static TestHelper helper;
    private static CompareToEqualsCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new CompareToEqualsCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when Comparable class also overrides equals")
    void analyze_comparableWithEquals_returnsPass() {
        var sootClass = helper.fixture("ComparableWithEquals");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when class does not implement Comparable")
    void analyze_notComparable_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag Comparable class that does not override equals")
    void analyze_comparableWithoutEquals_returnsViolation() {
        var sootClass = helper.fixture("ComparableWithoutEquals");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("Comparable"),
                "Violation should mention Comparable");
        assertTrue(violation.message().contains("equals"),
                "Violation should mention equals");
    }

    // === METADATA ===

    @Test
    @DisplayName("Guideline name should be 'compareTo/equals Consistency Check'")
    void name_returnsExpectedName() {
        assertEquals("compareTo/equals Consistency Check", check.name());
    }
}
