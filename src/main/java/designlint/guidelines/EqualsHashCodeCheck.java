package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #2: equals/hashCode Contract
 *
 * From the requirements:
 *   "If a class overrides the equals() method of java.lang.Object, it should also
 *    override the hashCode() method of java.lang.Object, and vice versa."
 *
 * WHY THIS MATTERS:
 * The contract between equals() and hashCode() states that if two objects are equal
 * (via equals()), they MUST have the same hashCode(). If you override equals() but
 * not hashCode(), objects that are "equal" could end up in different hash buckets.
 * This breaks HashMap, HashSet, and basically any hash-based collection.
 *
 * Classic bug: you put an object in a HashSet, then can't find it because the
 * hashCode() is wrong. These bugs are incredibly painful to track down.
 *
 * WHAT WE CHECK:
 * - Does the class override equals(Object)? (method with that exact signature)
 * - Does the class override hashCode()? (no-arg method returning int)
 * - If one is overridden but not the other → violation
 */
public class EqualsHashCodeCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "equals/hashCode Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public String description() {
        return "Verifies that if a class overrides equals(), it also overrides hashCode(), " +
               "and vice versa. Violating this contract breaks hash-based collections.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        boolean hasEquals = false;
        boolean hasHashCode = false;

        // Walk through all methods declared in THIS class (not inherited ones).
        // We're looking for overrides — methods that match the signatures from Object.
        for (SootMethod method : sootClass.getMethods()) {
            String methodName = method.getName();
            int paramCount = method.getParameterCount();

            // equals signature: public boolean equals(Object obj)
            // In bytecode, the parameter type is java.lang.Object
            if (methodName.equals("equals") && paramCount == 1) {
                String paramType = method.getParameterTypes().get(0).toString();
                if (paramType.equals("java.lang.Object")) {
                    hasEquals = true;
                }
            }

            // hashCode signature: public int hashCode()
            if (methodName.equals("hashCode") && paramCount == 0) {
                hasHashCode = true;
            }
        }

        // Both overridden or neither overridden → all good
        if (hasEquals == hasHashCode) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // One without the other → violation
        if (hasEquals && !hasHashCode) {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    "Class overrides equals() but NOT hashCode(). " +
                    "This will cause incorrect behavior with HashMap, HashSet, etc. " +
                    "Add a hashCode() override that is consistent with equals()."
            ));
        } else {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    "Class overrides hashCode() but NOT equals(). " +
                    "These methods must be overridden together to maintain the " +
                    "equals/hashCode contract."
            ));
        }
    }
}
