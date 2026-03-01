package designlint.core;

/**
 * Represents the result of running a single design guideline check against a class.
 *
 * === MODERN JAVA CONCEPTS ===
 *
 * "sealed interface" (Java 17): This restricts which classes can implement this interface.
 * Only Pass and Violation (declared with "permits") are allowed. This gives us exhaustive
 * pattern matching — the compiler knows every possible subtype. Think of it like an enum
 * but where each variant can carry different data.
 *
 * "record" (Java 16): A compact way to declare immutable data classes. The compiler
 * auto-generates the constructor, getters, equals(), hashCode(), and toString().
 * In Python terms, a record is like a @dataclass(frozen=True).
 *
 * So instead of writing 50+ lines of boilerplate (fields, constructor, getters, equals,
 * hashCode, toString), each record below is just one line of real content.
 */
public sealed interface AnalysisResult permits AnalysisResult.Pass, AnalysisResult.Violation {

    /** The fully qualified class name that was analyzed. */
    String className();

    /** The name of the design guideline that was checked. */
    String guidelineName();

    /** Whether this result represents a passing check. */
    default boolean passed() {
        return this instanceof Pass;
    }

    /**
     * A class passed a design guideline check. No issues found.
     *
     * @param className     the fully qualified name of the class (e.g., "com.example.MyClass")
     * @param guidelineName the name of the guideline that was satisfied
     */
    record Pass(String className, String guidelineName) implements AnalysisResult {}

    /**
     * A class violated a design guideline. Contains a human-readable explanation.
     *
     * @param className     the fully qualified name of the class
     * @param guidelineName the name of the guideline that was violated
     * @param message       a description of what's wrong and ideally how to fix it
     */
    record Violation(String className, String guidelineName, String message) implements AnalysisResult {}
}
