package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EmptyInterfaceCheck guideline.
 *
 * Covers:
 *   - Empty interface (no methods) → violation
 *   - Interface with methods → pass
 *   - Regular class (not an interface) → pass
 */
@DisplayName("EmptyInterfaceCheck")
class EmptyInterfaceCheckTest {

    private static TestHelper helper;
    private static EmptyInterfaceCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new EmptyInterfaceCheck();
    }

    @Test
    @DisplayName("Should flag an interface with no methods")
    void analyze_emptyInterface_returnsViolation() {
        var sootClass = helper.fixture("EmptyMarkerInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("marker interface"),
                "Violation should mention marker interface");
    }

    @Test
    @DisplayName("Should pass an interface that declares methods")
    void analyze_nonEmptyInterface_returnsPass() {
        var sootClass = helper.fixture("NonEmptyInterface");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass a regular class (not an interface)")
    void analyze_regularClass_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Guideline name should be 'Empty Interface Check'")
    void name_returnsExpectedName() {
        assertEquals("Empty Interface Check", check.name());
    }
}
