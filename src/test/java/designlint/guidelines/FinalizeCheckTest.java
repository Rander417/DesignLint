package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FinalizeCheck guideline.
 */
@DisplayName("FinalizeCheck")
class FinalizeCheckTest {

    private static TestHelper helper;
    private static FinalizeCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new FinalizeCheck();
    }

    @Test
    @DisplayName("Should flag a class that overrides finalize()")
    void analyze_hasFinalize_returnsViolation() {
        var sootClass = helper.fixture("HasFinalize");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("deprecated"),
                "Violation should mention deprecation");
    }

    @Test
    @DisplayName("Should pass a class that does not override finalize()")
    void analyze_noFinalize_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'finalize() Check'")
    void name_returnsExpectedName() {
        assertEquals("finalize() Check", check.name());
    }
}
