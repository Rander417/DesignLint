package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootField;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Design Guideline Check: Mutable Static Fields
 *
 * WHY THIS MATTERS:
 * Non-final static fields are global mutable state — the root of all evil in
 * concurrent programs. Every thread sees the same field, there's no synchronization
 * by default, and any code anywhere can change the value at any time. The result:
 *
 *   - Race conditions in multi-threaded code
 *   - Tight coupling (any class can read/write the state)
 *   - Testing nightmares (tests affect each other through shared state)
 *   - Memory leaks (static fields live for the entire JVM lifetime)
 *
 * Static FINAL fields (constants) are fine — they're immutable. The problem is
 * static fields that can be reassigned or mutated.
 *
 * WHAT WE CHECK:
 * We flag static fields that are NOT final. We make exceptions for:
 *   - serialVersionUID (static final long, already covered by SerializableCheck)
 *   - Fields in enum classes (compiler-generated)
 *   - Synthetic fields (compiler-generated, like $assertionsDisabled)
 */
public class MutableStaticCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Mutable Static Field Check";

    /** Fields that are commonly static non-final but are acceptable. */
    private static final Set<String> EXEMPT_FIELD_NAMES = Set.of(
            "serialVersionUID",
            "$assertionsDisabled"  // compiler-generated for assert statements
    );

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public Severity severity() {
        return Severity.WARNING;
    }

    @Override
    public String description() {
        return "Warns if a class has non-final static fields, which represent " +
               "global mutable state and cause concurrency and testing issues.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Skip enums — they have compiler-generated static fields
        if (sootClass.isEnum()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        List<AnalysisResult> violations = new ArrayList<>();

        for (SootField field : sootClass.getFields()) {
            // We only care about static, non-final fields
            if (!field.isStatic() || field.isFinal()) {
                continue;
            }

            // Skip known exempt fields
            if (EXEMPT_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            // Skip synthetic fields (compiler-generated)
            if (field.getName().startsWith("$")) {
                continue;
            }

            violations.add(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "Static field '" + field.getName() + "' is not final. " +
                    "Non-final static fields are global mutable state — they cause " +
                    "race conditions in concurrent code and make testing unreliable. " +
                    "Make it final, or convert to an instance field if shared state " +
                    "is not the intent."
            ));
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }
}
