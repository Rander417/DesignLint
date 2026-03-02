package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #9: compareTo/equals Consistency
 *
 * WHY THIS MATTERS:
 * The Comparable interface contract (per the Javadoc) "strongly recommends" that
 * compareTo be consistent with equals. Specifically:
 *   (x.compareTo(y) == 0) should imply x.equals(y) == true
 *
 * When this is violated, sorted collections (TreeSet, TreeMap) and hashed
 * collections (HashSet, HashMap) behave differently for the same objects.
 * Example: TreeSet uses compareTo to determine uniqueness, while HashSet uses
 * equals/hashCode. If compareTo returns 0 but equals returns false, an element
 * might be "unique" in a HashSet but "duplicate" in a TreeSet.
 *
 * This is a structural check — we can't prove mathematical consistency between
 * compareTo and equals at the bytecode level. But we CAN flag the suspicious case:
 * a class that implements Comparable but does NOT override equals(Object).
 * If you care enough to define ordering, you should care enough to define equality.
 *
 * WHAT WE CHECK:
 * 1. Does the class implement java.lang.Comparable?
 * 2. If yes, does it override equals(Object)?
 * 3. If not → violation (probable inconsistency)
 *
 * See Effective Java, Item 14: "Consider implementing Comparable."
 */
public class CompareToEqualsCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "compareTo/equals Consistency Check";

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
        return "Warns if a class implements Comparable but does not override equals(). " +
               "The compareTo and equals contracts should be consistent.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Check if the class implements java.lang.Comparable
        boolean implementsComparable = sootClass.getInterfaces().stream()
                .map(ClassType::getFullyQualifiedName)
                .anyMatch(name -> name.equals("java.lang.Comparable"));

        if (!implementsComparable) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Class implements Comparable — check if it overrides equals(Object)
        boolean hasEquals = sootClass.getMethods().stream()
                .anyMatch(m -> m.getName().equals("equals")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes().get(0).toString().equals("java.lang.Object"));

        if (hasEquals) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return List.of(new AnalysisResult.Violation(
                className,
                GUIDELINE_NAME,
                severity(),
                "Class implements Comparable but does not override equals(). " +
                "This likely means compareTo and equals are inconsistent: " +
                "TreeSet/TreeMap (which use compareTo) and HashSet/HashMap (which " +
                "use equals) will behave differently for objects of this class. " +
                "Override equals() to be consistent with compareTo()."
        ));
    }
}
