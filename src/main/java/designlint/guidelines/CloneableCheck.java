package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import sootup.core.model.SootClass;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #1: Cloneable Interface
 *
 * From the requirements:
 *   "It is not recommended that classes implement the java.lang.Cloneable interface
 *    (and override the clone() method). Instead, a copy constructor must be used."
 *
 * WHY THIS IS BAD:
 * The Cloneable interface in Java is widely considered a design mistake. It's a "marker"
 * interface (has no methods!) that magically changes the behavior of Object.clone().
 * The clone mechanism bypasses constructors, doesn't work well with final fields,
 * and the contract is poorly specified. Effective Java (Josh Bloch) recommends using
 * copy constructors or copy factory methods instead.
 *
 * WHAT WE CHECK:
 * Simply whether the class's interface list includes java.lang.Cloneable.
 */
public class CloneableCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Cloneable Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public String description() {
        return "Warns if a class implements java.lang.Cloneable. " +
               "Copy constructors should be used instead of clone().";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Check if any of the directly implemented interfaces is java.lang.Cloneable.
        // getInterfaces() returns the interfaces from the "implements" clause.
        boolean implementsCloneable = sootClass.getInterfaces().stream()
                .map(ClassType::getFullyQualifiedName)
                .anyMatch(name -> name.equals("java.lang.Cloneable"));

        if (implementsCloneable) {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    "Class implements java.lang.Cloneable. This is discouraged — " +
                    "consider using a copy constructor instead. " +
                    "See Effective Java, Item 13: 'Override clone judiciously'."
            ));
        }

        return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
    }
}
