package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OptionalMisuseCheck guideline.
 */
@DisplayName("OptionalMisuseCheck")
class OptionalMisuseCheckTest {

    private static TestHelper helper;
    private static OptionalMisuseCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new OptionalMisuseCheck();
    }

    @Test
    @DisplayName("Should flag Optional used as field and parameter")
    void analyze_optionalFieldAndParam_returnsViolations() {
        var sootClass = helper.fixture("OptionalAsFieldAndParam");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        long violationCount = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .count();
        assertTrue(violationCount >= 2,
                "Should flag at least 2 violations (field + parameter), got " + violationCount);

        // Check that we flag both kinds
        var messages = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> ((AnalysisResult.Violation) r).message())
                .toList();

        boolean flagsField = messages.stream().anyMatch(m -> m.contains("Field"));
        boolean flagsParam = messages.stream().anyMatch(m -> m.contains("parameter"));
        assertTrue(flagsField, "Should flag Optional field");
        assertTrue(flagsParam, "Should flag Optional parameter");
    }

    @Test
    @DisplayName("Should pass class that uses Optional only as return type")
    void analyze_optionalReturnOnly_returnsPass() {
        var sootClass = helper.fixture("OptionalReturnOnly");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Optional as return type only should pass");
    }

    @Test
    @DisplayName("Should pass a class with no Optional usage")
    void analyze_noOptional_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Optional Misuse Check'")
    void name_returnsExpectedName() {
        assertEquals("Optional Misuse Check", check.name());
    }
}
