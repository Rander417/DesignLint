package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EqualsPatternCheck guideline.
 *
 * This is the most complex check — it walks the Jimple IR of the equals() method body
 * looking for the recommended pattern: null check → instanceof → cast.
 *
 * We test several scenarios:
 *   - Full correct pattern → pass
 *   - No equals() override at all → pass (check doesn't apply)
 *   - Missing null check → violation
 *   - Missing instanceof → violation
 *   - Raw cast with nothing → multiple violations
 */
@DisplayName("EqualsPatternCheck")
class EqualsPatternCheckTest {

    private static TestHelper helper;
    private static EqualsPatternCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new EqualsPatternCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when equals() follows the full recommended pattern")
    void analyze_goodPattern_returnsPass() {
        var sootClass = helper.fixture("GoodEqualsPattern");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size(), "Good pattern should produce a single Pass result");
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when class does not override equals() at all")
    void analyze_noEqualsOverride_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0),
                "A class without equals() should pass — the check simply doesn't apply");
    }

    @Test
    @DisplayName("Should pass when class has correct equals pattern with both methods")
    void analyze_bothEqualsAndHashCode_returnsPass() {
        var sootClass = helper.fixture("BothEqualsAndHashCode");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag equals() that is missing the null check")
    void analyze_missingNullCheck_returnsViolation() {
        var sootClass = helper.fixture("EqualsMissingNullCheck");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        // Should have at least one violation about null check
        assertTrue(results.stream().anyMatch(r -> r instanceof AnalysisResult.Violation),
                "Missing null check should produce a violation");

        boolean mentionsNull = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> ((AnalysisResult.Violation) r).message())
                .anyMatch(msg -> msg.toLowerCase().contains("null"));
        assertTrue(mentionsNull, "Violation message should mention the missing null check");
    }

    @Test
    @DisplayName("Should flag equals() with raw cast (no null check, no instanceof)")
    void analyze_rawCast_returnsMultipleViolations() {
        var sootClass = helper.fixture("EqualsRawCast");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        long violationCount = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .count();

        assertTrue(violationCount >= 2,
                "Raw cast (no null check AND no instanceof) should produce at least 2 violations, " +
                "got " + violationCount);
    }

    @Test
    @DisplayName("Should flag missing pattern elements in the all-violations fixture")
    void analyze_allViolations_returnsPatternViolations() {
        var sootClass = helper.fixture("AllViolations");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertTrue(results.stream().anyMatch(r -> r instanceof AnalysisResult.Violation),
                "AllViolations class should trigger pattern violations");
    }

    // === METADATA ===

    @Test
    @DisplayName("Guideline name should be 'equals() Pattern Check'")
    void name_returnsExpectedName() {
        assertEquals("equals() Pattern Check", check.name());
    }

    @Test
    @DisplayName("Description should mention the recommended pattern elements")
    void description_mentionsPatternElements() {
        String desc = check.description();
        assertTrue(desc.contains("null"), "Description should mention null check");
        assertTrue(desc.contains("instanceof"), "Description should mention instanceof");
        assertTrue(desc.contains("cast"), "Description should mention cast");
    }
}
