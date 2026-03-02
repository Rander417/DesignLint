package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check: Missing @FunctionalInterface Annotation
 *
 * WHY THIS MATTERS:
 * A "functional interface" in Java is any interface with exactly one abstract method.
 * These can be used as lambda targets:
 *
 *   interface Validator { boolean validate(String input); }
 *   Validator v = input -> input.length() > 0;  // lambda works!
 *
 * The @FunctionalInterface annotation doesn't change behavior — lambdas work
 * without it. But it serves as documentation AND as a safety net: if someone
 * later adds a second abstract method, the compiler will flag it as an error
 * instead of silently breaking every lambda that uses the interface.
 *
 * Think of @FunctionalInterface like @Override — technically optional,
 * but omitting it throws away free compile-time verification.
 *
 * WHAT WE CHECK:
 * 1. Is this class an interface?
 * 2. Does it have exactly one abstract method?
 * 3. Does it lack the @FunctionalInterface annotation?
 *
 * We check for the annotation by looking for "FunctionalInterface" in the
 * class's annotation set. We skip well-known JDK functional interfaces since
 * those aren't the user's code.
 */
public class FunctionalInterfaceCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "@FunctionalInterface Check";

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
        return "Warns if an interface has exactly one abstract method but is missing " +
               "the @FunctionalInterface annotation, which prevents accidental breakage.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Only applies to interfaces
        if (!sootClass.isInterface()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Skip standard library interfaces — not the user's code
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Count abstract methods (exclude static and default methods)
        long abstractMethodCount = sootClass.getMethods().stream()
                .filter(m -> !m.isStatic())
                .filter(SootMethod::isAbstract)
                .count();

        // Not a functional interface candidate if != 1 abstract method
        if (abstractMethodCount != 1) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // It's a SAM interface — check for the @FunctionalInterface annotation.
        // SootUp's base SootClass doesn't expose annotations, but JavaSootClass
        // (which JavaView actually returns) does. We cast and check.
        boolean hasAnnotation = false;
        try {
            if (sootClass instanceof JavaSootClass javaSootClass) {
                for (var annotation : javaSootClass.getAnnotations(java.util.Optional.of(view))) {
                    if (annotation.getAnnotation().getFullyQualifiedName()
                            .equals("java.lang.FunctionalInterface")) {
                        hasAnnotation = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // If annotation inspection fails, give the benefit of the doubt
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        if (hasAnnotation) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return List.of(new AnalysisResult.Violation(
                className,
                GUIDELINE_NAME,
                severity(),
                "Interface has exactly one abstract method, making it usable as a " +
                "lambda target, but is missing the @FunctionalInterface annotation. " +
                "Adding @FunctionalInterface documents intent and prevents accidental " +
                "breakage if someone adds a second abstract method later."
        ));
    }
}
