package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MutableStaticCheck guideline.
 */
@DisplayName("MutableStaticCheck")
class MutableStaticCheckTest {

    private static TestHelper helper;
    private static MutableStaticCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new MutableStaticCheck();
    }

    @Test
    @DisplayName("Should flag non-final static fields")
    void analyze_mutableStatic_returnsViolations() {
        var sootClass = helper.fixture("MutableStaticFields");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        long violationCount = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .count();
        assertTrue(violationCount >= 2,
                "Should flag at least 2 mutable static fields, got " + violationCount);
    }

    @Test
    @DisplayName("Should pass class with only final static fields")
    void analyze_finalStatic_returnsPass() {
        var sootClass = helper.fixture("FinalStaticFields");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Final static fields should pass");
    }

    @Test
    @DisplayName("Should pass a class with no static fields")
    void analyze_noStaticFields_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Mutable Static Field Check'")
    void name_returnsExpectedName() {
        assertEquals("Mutable Static Field Check", check.name());
    }
}
