package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the UtilityClassConstructorCheck guideline.
 */
@DisplayName("UtilityClassConstructorCheck")
class UtilityClassConstructorCheckTest {

    private static TestHelper helper;
    private static UtilityClassConstructorCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new UtilityClassConstructorCheck();
    }

    @Test
    @DisplayName("Should flag utility class with public/default constructor")
    void analyze_utilityWithPublicCtor_returnsViolation() {
        var sootClass = helper.fixture("UtilityClassPublicCtor");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("private"),
                "Violation should suggest a private constructor");
    }

    @Test
    @DisplayName("Should pass utility class with private constructor")
    void analyze_utilityWithPrivateCtor_returnsPass() {
        var sootClass = helper.fixture("UtilityClassPrivateCtor");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass a normal class with instance methods")
    void analyze_normalClass_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass an interface")
    void analyze_interface_returnsPass() {
        var sootClass = helper.fixture("NonEmptyInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Utility Class Constructor Check'")
    void name_returnsExpectedName() {
        assertEquals("Utility Class Constructor Check", check.name());
    }
}
