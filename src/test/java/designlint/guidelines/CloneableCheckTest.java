package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CloneableCheck guideline.
 *
 * === JUNIT 5 CONCEPTS (for the Python-to-Java refresher) ===
 *
 * JUnit 5 is to Java what pytest is to Python. Key annotations:
 *
 * @Test           — marks a method as a test (like a function starting with test_ in pytest)
 * @BeforeAll      — runs once before ALL tests in this class (like a module-level fixture)
 * @BeforeEach     — runs before EACH test (like pytest's setup_method)
 * @DisplayName    — gives the test a human-readable name in reports
 *
 * Assertions work similarly to Python's assert, but with methods:
 *   assertEquals(expected, actual)   — like assert expected == actual
 *   assertTrue(condition)            — like assert condition
 *   assertFalse(condition)           — like assert not condition
 *   assertInstanceOf(Type, obj)      — like assert isinstance(obj, Type)
 *
 * The test methods are intentionally named with a readable pattern:
 *   methodUnderTest_scenario_expectedBehavior
 * This makes test reports self-documenting.
 */
@DisplayName("CloneableCheck")
class CloneableCheckTest {

    private static TestHelper helper;
    private static CloneableCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new CloneableCheck();
    }

    @Test
    @DisplayName("Should flag a class that implements Cloneable")
    void analyze_classImplementsCloneable_returnsViolation() {
        var sootClass = helper.fixture("ImplementsCloneable");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size(), "Should produce exactly one result");
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("Cloneable"),
                "Violation message should mention Cloneable");
    }

    @Test
    @DisplayName("Should pass a class that does NOT implement Cloneable")
    void analyze_classWithoutCloneable_returnsPass() {
        var sootClass = helper.fixture("NoCloneable");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass a plain class with no interfaces at all")
    void analyze_plainClass_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should flag Cloneable even when class has multiple violations")
    void analyze_allViolationsClass_flagsCloneable() {
        var sootClass = helper.fixture("AllViolations");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));
    }

    @Test
    @DisplayName("Result should include the correct class name")
    void analyze_anyClass_resultContainsClassName() {
        var sootClass = helper.fixture("ImplementsCloneable");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals("designlint.fixtures.ImplementsCloneable", results.get(0).className());
    }

    @Test
    @DisplayName("Result should include the guideline name")
    void analyze_anyClass_resultContainsGuidelineName() {
        var sootClass = helper.fixture("ImplementsCloneable");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals("Cloneable Check", results.get(0).guidelineName());
    }
}
