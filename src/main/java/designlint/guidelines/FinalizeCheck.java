package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #6: finalize() Override Warning
 *
 * WHY THIS MATTERS:
 * Object.finalize() was Java's original attempt at destructor-like cleanup.
 * It was a well-intentioned disaster. Problems include:
 *
 *   - No guarantee WHEN (or even IF) it will be called
 *   - Severe GC performance penalty (finalizable objects survive an extra GC cycle)
 *   - Can accidentally "resurrect" dead objects
 *   - Exception handling is silently swallowed
 *   - Security vulnerabilities (finalizer attacks)
 *
 * Deprecated since Java 9, deprecated-for-removal since Java 18. The modern
 * alternatives are try-with-resources (for most cleanup) and java.lang.ref.Cleaner
 * (for the rare cases where you genuinely need posr-mortem cleanup).
 *
 * Josh Bloch's Effective Java (Item 8) is titled simply: "Avoid finalizers and cleaners."
 * When Josh Bloch tells you to avoid something, you avoid it.
 *
 * WHAT WE CHECK:
 * Simply whether the class declares a method named finalize() with no parameters.
 */
public class FinalizeCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "finalize() Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public String description() {
        return "Warns if a class overrides finalize(). This method is deprecated " +
               "and causes GC performance issues. Use try-with-resources or Cleaner instead.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        boolean hasFinalize = sootClass.getMethods().stream()
                .anyMatch(m -> m.getName().equals("finalize") && m.getParameterCount() == 0);

        if (hasFinalize) {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    "Class overrides finalize(). This method is deprecated since Java 9 " +
                    "and causes unpredictable GC behavior, performance degradation, and " +
                    "potential security vulnerabilities. Use try-with-resources for " +
                    "deterministic cleanup, or java.lang.ref.Cleaner for rare edge cases. " +
                    "See Effective Java, Item 8: 'Avoid finalizers and cleaners.'"
            ));
        }

        return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
    }
}
