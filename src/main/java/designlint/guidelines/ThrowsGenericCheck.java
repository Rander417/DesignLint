package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Design Guideline Check: Throwing Generic Exception
 *
 * WHY THIS MATTERS:
 * When a method declares "throws Exception" or "throws Throwable", it tells
 * callers absolutely nothing about what can actually go wrong. It forces them
 * to either catch Exception (which is itself an anti-pattern — see CatchAllCheck)
 * or propagate the generic declaration up the call chain, infecting every caller.
 *
 * Specific exception types are documentation. "throws IOException" tells you
 * a file or network operation might fail. "throws Exception" tells you...
 * something might go wrong. Somewhere. Somehow. Good luck.
 *
 * WHAT WE CHECK:
 * We examine each method's throws clause (the declared exceptions) for
 * java.lang.Exception or java.lang.Throwable. We skip main() methods since
 * "throws Exception" on main is a common and harmless convention.
 */
public class ThrowsGenericCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Generic throws Check";

    private static final Set<String> OVERLY_BROAD_TYPES = Set.of(
            "java.lang.Exception",
            "java.lang.Throwable"
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
        return "Warns if a method declares 'throws Exception' or 'throws Throwable' " +
               "instead of specific exception types.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();
        List<AnalysisResult> violations = new ArrayList<>();

        for (SootMethod method : sootClass.getMethods()) {
            // Skip main() — "throws Exception" on main is a common harmless idiom
            if (method.getName().equals("main") && method.isStatic()) {
                continue;
            }

            // Check the declared exceptions in the throws clause
            for (ClassType exceptionType : method.getExceptionSignatures()) {
                String exTypeName = exceptionType.getFullyQualifiedName();

                if (OVERLY_BROAD_TYPES.contains(exTypeName)) {
                    violations.add(new AnalysisResult.Violation(
                            className,
                            GUIDELINE_NAME,
                            severity(),
                            "Method " + method.getName() + "() declares 'throws " +
                            exceptionType.getClassName() + "'. This is too broad and " +
                            "forces callers to handle or propagate a generic exception. " +
                            "Declare specific exception types that reflect what can " +
                            "actually go wrong."
                    ));
                }
            }
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }
}
