package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ToStringCheck guideline.
 *
 * Covers:
 *   - Class with fields AND toString → pass
 *   - Class with fields but NO toString → violation
 *   - Class with only static fields (no instance fields) → pass (exempt)
 *   - Class with no fields at all → pass (exempt)
 */
@DisplayName("ToStringCheck")
class ToStringCheckTest {

    private static TestHelper helper;
    private static ToStringCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new ToStringCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when class has fields and overrides toString()")
    void analyze_hasToString_returnsPass() {
        var sootClass = helper.fixture("HasToString");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when class has only static fields (utility class)")
    void analyze_staticOnlyFields_returnsPass() {
        var sootClass = helper.fixture("StaticOnlyClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0),
                "Utility classes with only static fields should be exempt");
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag a class with instance fields but no toString()")
    void analyze_fieldsWithoutToString_returnsViolation() {
        // PlainClass has a 'data' field but no toString() override
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("toString"),
                "Violation should mention toString()");
    }

    // === METADATA ===

    @Test
    @DisplayName("Guideline name should be 'toString() Check'")
    void name_returnsExpectedName() {
        assertEquals("toString() Check", check.name());
    }

    @Test
    @DisplayName("Description should mention debugging")
    void description_mentionsDebugging() {
        assertTrue(check.description().contains("debugging"));
    }
}
