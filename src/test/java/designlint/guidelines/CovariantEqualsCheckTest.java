package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CovariantEqualsCheck guideline.
 *
 * Covers:
 *   - equals(SpecificType) without equals(Object) → violation (bug)
 *   - equals(SpecificType) WITH equals(Object) → violation (code smell)
 *   - No equals at all → pass
 *   - Proper equals(Object) only → pass
 */
@DisplayName("CovariantEqualsCheck")
class CovariantEqualsCheckTest {

    private static TestHelper helper;
    private static CovariantEqualsCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new CovariantEqualsCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when class has no equals method at all")
    void analyze_noEquals_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when class has only proper equals(Object)")
    void analyze_properEqualsOnly_returnsPass() {
        var sootClass = helper.fixture("GoodEqualsPattern");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag covariant equals without proper equals(Object)")
    void analyze_covariantOnly_returnsViolation() {
        var sootClass = helper.fixture("CovariantEqualsOnly");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("does NOT override"),
                "Should indicate the proper override is missing");
    }

    @Test
    @DisplayName("Should flag covariant equals even when proper equals(Object) exists")
    void analyze_covariantWithProper_returnsViolation() {
        var sootClass = helper.fixture("CovariantEqualsWithProper");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("in addition to"),
                "Should indicate it's a code smell, not a bug");
    }

    // === METADATA ===

    @Test
    @DisplayName("Guideline name should be 'Covariant equals() Check'")
    void name_returnsExpectedName() {
        assertEquals("Covariant equals() Check", check.name());
    }
}
