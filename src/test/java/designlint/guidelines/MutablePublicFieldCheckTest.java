package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MutablePublicFieldCheck guideline.
 */
@DisplayName("MutablePublicFieldCheck")
class MutablePublicFieldCheckTest {

    private static TestHelper helper;
    private static MutablePublicFieldCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new MutablePublicFieldCheck();
    }

    @Test
    @DisplayName("Should flag public non-final instance fields")
    void analyze_publicMutableFields_returnsViolations() {
        var sootClass = helper.fixture("PublicMutableFields");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        long violationCount = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .count();
        assertTrue(violationCount >= 3,
                "Should flag at least 3 public mutable fields, got " + violationCount);
    }

    @Test
    @DisplayName("Should pass class with private fields and accessors")
    void analyze_encapsulatedFields_returnsPass() {
        var sootClass = helper.fixture("EncapsulatedFields");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Private fields with accessors should pass");
    }

    @Test
    @DisplayName("Should pass a class with no fields")
    void analyze_noFields_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Mutable Public Field Check'")
    void name_returnsExpectedName() {
        assertEquals("Mutable Public Field Check", check.name());
    }
}
