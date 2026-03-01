package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EqualsHashCodeCheck guideline.
 *
 * Tests the four possible states:
 *   - equals + hashCode    → pass (both overridden)
 *   - neither              → pass (both inherited)
 *   - equals only          → violation
 *   - hashCode only        → violation
 */
@DisplayName("EqualsHashCodeCheck")
class EqualsHashCodeCheckTest {

    private static TestHelper helper;
    private static EqualsHashCodeCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new EqualsHashCodeCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when both equals() and hashCode() are overridden")
    void analyze_bothOverridden_returnsPass() {
        var sootClass = helper.fixture("BothEqualsAndHashCode");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when neither equals() nor hashCode() is overridden")
    void analyze_neitherOverridden_returnsPass() {
        var sootClass = helper.fixture("NeitherEqualsNorHashCode");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass for a plain class with no overrides")
    void analyze_plainClass_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag equals() without hashCode()")
    void analyze_equalsWithoutHashCode_returnsViolation() {
        var sootClass = helper.fixture("EqualsWithoutHashCode");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("equals()"),
                "Message should mention equals()");
        assertTrue(violation.message().contains("NOT hashCode()"),
                "Message should mention missing hashCode()");
    }

    @Test
    @DisplayName("Should flag hashCode() without equals()")
    void analyze_hashCodeWithoutEquals_returnsViolation() {
        var sootClass = helper.fixture("HashCodeWithoutEquals");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("hashCode()"),
                "Message should mention hashCode()");
        assertTrue(violation.message().contains("NOT equals()"),
                "Message should mention missing equals()");
    }

    @Test
    @DisplayName("Should catch equals-without-hashCode in a multi-violation class")
    void analyze_allViolationsClass_flagsMissingHashCode() {
        var sootClass = helper.fixture("AllViolations");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));
    }

    // === METADATA CHECKS ===

    @Test
    @DisplayName("Guideline name should be 'equals/hashCode Check'")
    void name_returnsExpectedName() {
        assertEquals("equals/hashCode Check", check.name());
    }

    @Test
    @DisplayName("Description should not be empty")
    void description_isNotEmpty() {
        assertFalse(check.description().isEmpty());
    }
}
