package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #5: Covariant equals() Method
 *
 * WHY THIS MATTERS:
 * One of the most common Java mistakes: defining equals(MyClass) instead of
 * equals(Object). This DOESN'T override Object.equals() — it overloads it.
 * The result is deeply confusing: your equals works fine when you call it directly,
 * but collections like HashSet and HashMap call equals(Object), which falls through
 * to Object's identity-based comparison.
 *
 * Classic scenario:
 *   class Point {
 *       boolean equals(Point other) { ... }  // WRONG! This is an overload
 *   }
 *   Set<Point> set = new HashSet<>();
 *   set.add(new Point(1, 2));
 *   set.contains(new Point(1, 2));  // returns false! Uses Object.equals()
 *
 * WHAT WE CHECK:
 * We look for a method named "equals" that takes exactly one parameter whose type
 * is NOT java.lang.Object. If such a method exists AND there's no proper
 * equals(Object) override, that's a violation.
 *
 * If the class has BOTH equals(MyClass) and equals(Object), we still flag the
 * covariant version as a warning since it's a potential source of confusion,
 * but the message is softer.
 */
public class CovariantEqualsCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Covariant equals() Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "Detects equals() methods that take a specific type instead of Object, " +
               "which overloads rather than overrides Object.equals().";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        boolean hasProperEquals = false;
        boolean hasCovariantEquals = false;
        String covariantParamType = null;

        for (SootMethod method : sootClass.getMethods()) {
            if (!method.getName().equals("equals") || method.getParameterCount() != 1) {
                continue;
            }

            String paramType = method.getParameterTypes().get(0).toString();

            if (paramType.equals("java.lang.Object")) {
                hasProperEquals = true;
            } else {
                hasCovariantEquals = true;
                covariantParamType = paramType;
            }
        }

        if (!hasCovariantEquals) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Covariant equals exists — how bad is it?
        if (!hasProperEquals) {
            // No proper equals(Object) override at all — this is a real bug
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "Class defines equals(" + covariantParamType + ") but does NOT override " +
                    "equals(Object). This overloads rather than overrides Object.equals(), " +
                    "so collections like HashSet and HashMap will NOT use your equals logic. " +
                    "Change the parameter type to Object and add a cast inside the method."
            ));
        } else {
            // Has both — not broken, but still a code smell
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    Severity.ADVISORY,
                    "Class defines equals(" + covariantParamType + ") in addition to " +
                    "equals(Object). While the proper override exists, the covariant " +
                    "overload can cause confusion and maintenance issues. Consider " +
                    "removing the type-specific equals method."
            ));
        }
    }
}
