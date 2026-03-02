package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check: Utility Class with Public Constructor
 *
 * WHY THIS MATTERS:
 * A utility class (sometimes called a "helper class") contains only static methods
 * and static fields — things like Collections, Math, Arrays, Objects. These classes
 * are not meant to be instantiated. But Java gives every class a default public
 * constructor if you don't declare one, which means someone could write:
 *
 *   Math math = new Math();  // Compiles! But makes zero sense.
 *
 * The fix is simple: declare a private constructor. This prevents instantiation
 * and also prevents subclassing (which also makes no sense for utility classes).
 *
 * WHAT WE CHECK:
 * 1. Is the class non-abstract, non-interface, non-enum?
 * 2. Are ALL declared methods static (excluding constructors)?
 * 3. Does the class have at least one static method (not just an empty class)?
 * 4. Is the constructor public (or default)?
 *
 * If all conditions are true, the class is a utility class with a public constructor.
 */
public class UtilityClassConstructorCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Utility Class Constructor Check";

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
        return "Warns if a utility class (only static methods) has a public or default " +
               "constructor. Utility classes should declare a private constructor.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Skip interfaces, abstract classes, enums — not utility class candidates
        if (sootClass.isInterface() || sootClass.isAbstract() || sootClass.isEnum()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        boolean hasStaticMethod = false;
        boolean hasNonStaticMethod = false;
        boolean hasPublicConstructor = false;

        for (SootMethod method : sootClass.getMethods()) {
            String methodName = method.getName();

            // Check constructors
            if (methodName.equals("<init>")) {
                if (method.isPublic()) {
                    hasPublicConstructor = true;
                }
                continue;  // Don't count constructors as "methods"
            }

            // Skip static initializer
            if (methodName.equals("<clinit>")) {
                continue;
            }

            // Categorize real methods
            if (method.isStatic()) {
                hasStaticMethod = true;
            } else {
                hasNonStaticMethod = true;
            }
        }

        // It's a utility class if: has static methods, NO instance methods,
        // and has a public constructor
        if (hasStaticMethod && !hasNonStaticMethod && hasPublicConstructor) {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "Class contains only static methods but has a public constructor. " +
                    "This allows pointless instantiation. Add a private no-arg constructor " +
                    "to prevent instantiation: private " +
                    simpleClassName(className) + "() { throw new AssertionError(); }"
            ));
        }

        return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
    }

    /** Extract simple class name from fully qualified name. */
    private String simpleClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
}
