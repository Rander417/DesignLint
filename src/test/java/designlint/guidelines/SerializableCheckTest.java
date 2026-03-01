package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SerializableCheck guideline.
 *
 * Covers three scenarios:
 *   - Serializable WITH serialVersionUID → pass
 *   - Serializable WITHOUT serialVersionUID → violation
 *   - Not Serializable at all → pass
 */
@DisplayName("SerializableCheck")
class SerializableCheckTest {

    private static TestHelper helper;
    private static SerializableCheck check;

    @BeforeAll
    static void setUp() {
        helper = new TestHelper();
        check = new SerializableCheck();
    }

    // === PASSING CASES ===

    @Test
    @DisplayName("Should pass when Serializable class has serialVersionUID")
    void analyze_serializableWithUID_returnsPass() {
        var sootClass = helper.fixture("SerializableWithUID");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    @Test
    @DisplayName("Should pass when class does not implement Serializable")
    void analyze_notSerializable_returnsPass() {
        var sootClass = helper.fixture("PlainClass");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Pass.class, results.get(0));
    }

    // === VIOLATION CASES ===

    @Test
    @DisplayName("Should flag Serializable class missing serialVersionUID")
    void analyze_serializableWithoutUID_returnsViolation() {
        var sootClass = helper.fixture("SerializableWithoutUID");
        List<AnalysisResult> results = check.analyze(sootClass, helper.getView());

        assertEquals(1, results.size());
        assertInstanceOf(AnalysisResult.Violation.class, results.get(0));

        var violation = (AnalysisResult.Violation) results.get(0);
        assertTrue(violation.message().contains("serialVersionUID"),
                "Violation message should mention serialVersionUID");
    }

    // === METADATA ===

    @Test
    @DisplayName("Guideline name should be 'Serializable Check'")
    void name_returnsExpectedName() {
        assertEquals("Serializable Check", check.name());
    }

    @Test
    @DisplayName("Description should mention serialVersionUID")
    void description_mentionsSerialVersionUID() {
        assertTrue(check.description().contains("serialVersionUID"));
    }
}
