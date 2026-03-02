package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #10: toString() Not Overridden
 *
 * WHY THIS MATTERS:
 * When you don't override toString(), Object's default implementation produces:
 *   "com.example.MyClass@1a2b3c4d"
 *
 * This is useless for debugging, logging, and error messages. When something breaks
 * at 2am and your log says "Failed to process order: Order@7f31245a", you have
 * nothing to work with.
 *
 * This is a lower-severity guideline — not overriding toString() won't cause bugs.
 * But it's a strong code quality signal. Classes that model domain concepts (entities,
 * value objects, DTOs) should almost always have a meaningful toString().
 *
 * NOTE: We exempt certain classes where toString() doesn't make sense:
 *   - Interfaces (no implementation to override)
 *   - Abstract classes (subclasses should override)
 *   - Classes with no fields (nothing meaningful to print)
 *
 * See Effective Java, Item 12: "Always override toString."
 *
 * WHAT WE CHECK:
 * Does the class declare a toString() method (no parameters, returns String)?
 */
public class ToStringCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "toString() Check";

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
        return "Warns if a class with fields does not override toString(). " +
               "A meaningful toString() is essential for debugging and logging.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Skip interfaces and abstract classes — they shouldn't own toString()
        if (sootClass.isInterface() || sootClass.isAbstract()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Skip classes with no instance fields — nothing meaningful to print
        boolean hasInstanceFields = sootClass.getFields().stream()
                .anyMatch(f -> !f.isStatic());

        if (!hasInstanceFields) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Check if toString() is overridden
        boolean hasToString = sootClass.getMethods().stream()
                .anyMatch(m -> m.getName().equals("toString")
                        && m.getParameterCount() == 0);

        if (hasToString) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return List.of(new AnalysisResult.Violation(
                className,
                GUIDELINE_NAME,
                severity(),
                "Class has fields but does not override toString(). " +
                "The default Object.toString() produces unhelpful output like '" +
                className + "@1a2b3c'. Override toString() to include relevant " +
                "field values for better debugging and logging."
        ));
    }
}
