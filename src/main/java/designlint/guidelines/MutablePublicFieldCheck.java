package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootField;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Guideline Check: Mutable Public Fields
 *
 * WHY THIS MATTERS:
 * Public non-final fields expose a class's internal state directly. Any code
 * anywhere can read or modify the field, which means:
 *
 *   - No validation: You can't enforce invariants ("age must be positive")
 *   - No change notification: Observers can't react to state changes
 *   - No lazy computation: The value must always be materialized
 *   - Breaking encapsulation: You can never change the internal representation
 *     without breaking every caller
 *
 * Public FINAL fields on immutable types (like String or primitives) are
 * acceptable as constants. But public non-final fields are almost always a
 * design problem.
 *
 * WHAT WE CHECK:
 * We flag public instance fields that are not final. We exempt:
 *   - Static fields (covered by MutableStaticCheck)
 *   - Final fields (immutable binding, even if the object is mutable)
 *   - Fields in interfaces (implicitly public static final)
 */
public class MutablePublicFieldCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Mutable Public Field Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public Severity severity() {
        return Severity.ADVISORY;
    }

    @Override
    public String description() {
        return "Warns if a class has public non-final instance fields, which " +
               "break encapsulation and prevent validation or change tracking.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Interfaces only have public static final fields — skip
        if (sootClass.isInterface()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        List<AnalysisResult> violations = new ArrayList<>();

        for (SootField field : sootClass.getFields()) {
            // We want: public, NOT static, NOT final
            if (field.isPublic() && !field.isStatic() && !field.isFinal()) {
                violations.add(new AnalysisResult.Violation(
                        className,
                        GUIDELINE_NAME,
                        severity(),
                        "Field '" + field.getName() + "' is public and non-final. " +
                        "This exposes internal state and prevents validation, lazy " +
                        "computation, or change tracking. Make the field private and " +
                        "provide accessor methods, or make it final if it's a constant."
                ));
            }
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }
}
