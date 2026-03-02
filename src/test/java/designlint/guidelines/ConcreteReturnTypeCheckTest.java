package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ConcreteReturnTypeCheck guideline.
 *
 * Covers:
 *   - Public methods returning ArrayList/HashMap → violations
 *   - Public methods returning List/Map → pass
 *   - Class with no collection-returning methods → pass
 */
@DisplayName("ConcreteReturnTypeCheck")
class ConcreteReturnTypeCheckTest {

    private static TestHelper helper;
    private static ConcreteReturnTypeCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new ConcreteReturnTypeCheck();
    }

    @Test
    @DisplayName("Should flag public methods returning concrete collection types")
    void analyze_returnsConcreteCollection_returnsViolations() {
        var sootClass = helper.fixture("ReturnsConcreteCollection");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        long violationCount = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .count();
        assertTrue(violationCount >= 2,
                "Should flag at least 2 violations (ArrayList and HashMap), got " + violationCount);

        // Check that violations mention the concrete types
        boolean mentionsArrayList = results.stream()
                .filter(r -> r instanceof AnalysisResult.Violation)
                .map(r -> ((AnalysisResult.Violation) r).message())
                .anyMatch(msg -> msg.contains("ArrayList"));
        assertTrue(mentionsArrayList, "Should mention ArrayList in at least one violation");
    }

    @Test
    @DisplayName("Should pass public methods returning collection interfaces")
    void analyze_returnsInterfaceCollection_returnsPass() {
        var sootClass = helper.fixture("ReturnsInterfaceCollection");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        boolean allPass = results.stream()
                .allMatch(r -> r instanceof AnalysisResult.Pass);
        assertTrue(allPass, "Returning List/Map interfaces should pass");
    }

    @Test
    @DisplayName("Should pass a class with no collection-returning methods")
    void analyze_noCollections_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Concrete Return Type Check'")
    void name_returnsExpectedName() {
        assertEquals("Concrete Return Type Check", check.name());
    }
}
