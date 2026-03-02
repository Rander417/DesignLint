package designlint.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AnalysisResult sealed interface and its record implementations.
 *
 * These tests verify the behavior of Java records — since the compiler generates
 * equals(), hashCode(), and toString() for us, it's good practice to verify
 * they work as expected. Think of this as testing the "data model" layer.
 */
@DisplayName("AnalysisResult")
class AnalysisResultTest {

    // === Pass record ===

    @Test
    @DisplayName("Pass.passed() should return true")
    void pass_passed_returnsTrue() {
        var pass = new AnalysisResult.Pass("com.example.Foo", "Some Check");
        assertTrue(pass.passed());
    }

    @Test
    @DisplayName("Pass should store class name correctly")
    void pass_className_returnsConstructorValue() {
        var pass = new AnalysisResult.Pass("com.example.MyClass", "Check");
        assertEquals("com.example.MyClass", pass.className());
    }

    @Test
    @DisplayName("Pass should store guideline name correctly")
    void pass_guidelineName_returnsConstructorValue() {
        var pass = new AnalysisResult.Pass("Class", "My Guideline");
        assertEquals("My Guideline", pass.guidelineName());
    }

    @Test
    @DisplayName("Two Pass records with same data should be equal (record auto-generated equals)")
    void pass_equalRecords_areEqual() {
        var pass1 = new AnalysisResult.Pass("Foo", "Check");
        var pass2 = new AnalysisResult.Pass("Foo", "Check");
        assertEquals(pass1, pass2);
    }

    @Test
    @DisplayName("Two Pass records with different data should not be equal")
    void pass_differentRecords_areNotEqual() {
        var pass1 = new AnalysisResult.Pass("Foo", "Check");
        var pass2 = new AnalysisResult.Pass("Bar", "Check");
        assertNotEquals(pass1, pass2);
    }

    // === Violation record ===

    @Test
    @DisplayName("Violation.passed() should return false")
    void violation_passed_returnsFalse() {
        var violation = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "Something is wrong");
        assertFalse(violation.passed());
    }

    @Test
    @DisplayName("Violation should store the message correctly")
    void violation_message_returnsConstructorValue() {
        var violation = new AnalysisResult.Violation("Foo", "Check", Severity.WARNING, "Bad things happened");
        assertEquals("Bad things happened", violation.message());
    }

    @Test
    @DisplayName("Violation should store severity correctly")
    void violation_severity_returnsConstructorValue() {
        var violation = new AnalysisResult.Violation("Foo", "Check", Severity.ADVISORY, "Some suggestion");
        assertEquals(Severity.ADVISORY, violation.severity());
    }

    @Test
    @DisplayName("Violation with same data should be equal")
    void violation_equalRecords_areEqual() {
        var v1 = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "msg");
        var v2 = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "msg");
        assertEquals(v1, v2);
    }

    @Test
    @DisplayName("Violations with different severity should not be equal")
    void violation_differentSeverity_areNotEqual() {
        var v1 = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "msg");
        var v2 = new AnalysisResult.Violation("Foo", "Check", Severity.WARNING, "msg");
        assertNotEquals(v1, v2);
    }

    // === Sealed interface behavior ===

    @Test
    @DisplayName("Pass is an instance of AnalysisResult")
    void pass_isInstanceOfAnalysisResult() {
        AnalysisResult result = new AnalysisResult.Pass("Foo", "Check");
        assertInstanceOf(AnalysisResult.Pass.class, result);
    }

    @Test
    @DisplayName("Violation is an instance of AnalysisResult")
    void violation_isInstanceOfAnalysisResult() {
        AnalysisResult result = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "msg");
        assertInstanceOf(AnalysisResult.Violation.class, result);
    }

    @Test
    @DisplayName("toString() on Pass should contain class and guideline names")
    void pass_toString_containsFields() {
        var pass = new AnalysisResult.Pass("com.example.Foo", "My Check");
        String str = pass.toString();
        assertTrue(str.contains("com.example.Foo"), "toString should contain class name");
        assertTrue(str.contains("My Check"), "toString should contain guideline name");
    }

    @Test
    @DisplayName("toString() on Violation should contain message")
    void violation_toString_containsMessage() {
        var v = new AnalysisResult.Violation("Foo", "Check", Severity.ERROR, "broken!");
        assertTrue(v.toString().contains("broken!"));
    }

    // === Severity enum ===

    @Test
    @DisplayName("Severity display names should be human-friendly")
    void severity_displayNames() {
        assertEquals("Error", Severity.ERROR.displayName());
        assertEquals("Warning", Severity.WARNING.displayName());
        assertEquals("Advisory", Severity.ADVISORY.displayName());
    }

    @Test
    @DisplayName("Severity toString should match display name")
    void severity_toString_matchesDisplayName() {
        assertEquals("Error", Severity.ERROR.toString());
        assertEquals("Warning", Severity.WARNING.toString());
        assertEquals("Advisory", Severity.ADVISORY.toString());
    }
}
