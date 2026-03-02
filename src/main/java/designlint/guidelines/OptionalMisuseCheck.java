package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootField;
import sootup.core.model.SootMethod;
import sootup.core.types.Type;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Guideline Check: Optional Misuse
 *
 * WHY THIS MATTERS:
 * java.util.Optional was introduced in Java 8 with a specific, narrow purpose:
 * as a return type for methods that might not have a result. That's it.
 *
 * Using Optional elsewhere is an anti-pattern:
 *
 *   - Optional as a FIELD: Adds a wrapper object to every instance. Optional is
 *     not Serializable, which breaks serialization. It also obscures the class's
 *     actual state — is the field null, or is it an empty Optional? Now you have
 *     two kinds of "absent" to worry about.
 *
 *   - Optional as a METHOD PARAMETER: Forces callers to wrap values unnecessarily.
 *     Use method overloading or @Nullable instead. Brian Goetz (Java architect)
 *     has explicitly stated this was never the intended use.
 *
 * WHAT WE CHECK:
 * 1. Fields typed as java.util.Optional → violation
 * 2. Method parameters typed as java.util.Optional → violation
 * We skip constructors and synthetic methods.
 */
public class OptionalMisuseCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Optional Misuse Check";
    private static final String OPTIONAL_TYPE = "java.util.Optional";

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
        return "Warns if Optional is used as a field type or method parameter. " +
               "Optional should only be used as a method return type.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();
        List<AnalysisResult> violations = new ArrayList<>();

        // Check fields
        for (SootField field : sootClass.getFields()) {
            if (isOptionalType(field.getType())) {
                violations.add(new AnalysisResult.Violation(
                        className,
                        GUIDELINE_NAME,
                        severity(),
                        "Field '" + field.getName() + "' is typed as Optional. " +
                        "Optional is not intended for use as a field type — it's not " +
                        "Serializable, adds unnecessary wrapping, and creates ambiguity " +
                        "between null and empty. Use the raw type and handle nullability " +
                        "directly, or use @Nullable annotations."
                ));
            }
        }

        // Check method parameters
        for (SootMethod method : sootClass.getMethods()) {
            // Skip constructors and synthetic methods
            if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) {
                continue;
            }

            List<? extends Type> paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.size(); i++) {
                if (isOptionalType(paramTypes.get(i))) {
                    violations.add(new AnalysisResult.Violation(
                            className,
                            GUIDELINE_NAME,
                            severity(),
                            "Method " + method.getName() + "() has an Optional parameter " +
                            "(parameter " + (i + 1) + "). Optional should not be used as " +
                            "a method parameter — it forces callers to wrap values " +
                            "unnecessarily. Use method overloading or @Nullable instead."
                    ));
                }
            }
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }

    /**
     * Check if a type is java.util.Optional.
     * We use toString() since Type doesn't always expose getFullyQualifiedName().
     */
    private boolean isOptionalType(Type type) {
        return type.toString().equals(OPTIONAL_TYPE);
    }
}
