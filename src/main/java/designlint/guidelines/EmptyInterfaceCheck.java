package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.List;
import java.util.Set;

/**
 * Design Guideline Check: Empty Interface Warning
 *
 * WHY THIS MATTERS:
 * An interface with zero methods is a "marker interface" — it exists solely to
 * tag a class with a type. Java's own java.io.Serializable and java.lang.Cloneable
 * are marker interfaces, and both are widely considered design mistakes.
 *
 * The modern alternative is annotations. @Serializable, @Immutable, @ThreadSafe
 * can carry metadata, are processable at compile time, and don't pollute the
 * type hierarchy. If you're creating a new marker interface in 2026, you almost
 * certainly want an annotation instead.
 *
 * WHAT WE CHECK:
 * 1. Is this class an interface?
 * 2. Does it declare zero abstract methods? (We exclude default and static methods
 *    since those provide implementation, not contract.)
 * 3. If it's a known standard library marker interface, we skip it — no point
 *    flagging java.io.Serializable since the developer can't change the JDK.
 *
 * We only flag interfaces defined in the user's codebase, not standard library ones.
 */
public class EmptyInterfaceCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Empty Interface Check";

    /** Well-known marker interfaces we shouldn't flag — they're not the user's fault. */
    private static final Set<String> KNOWN_MARKERS = Set.of(
            "java.io.Serializable",
            "java.lang.Cloneable",
            "java.util.RandomAccess",
            "java.rmi.Remote",
            "java.util.EventListener"
    );

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
        return "Warns if an interface declares no methods (marker interface). " +
               "Consider using annotations instead for tagging classes.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Only applies to interfaces
        if (!sootClass.isInterface()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Skip well-known standard library marker interfaces
        if (KNOWN_MARKERS.contains(className)) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Count abstract methods (the actual contract).
        // Default methods and static methods provide implementation, not contract,
        // so we exclude them. An interface with only default/static methods and
        // zero abstract methods is still effectively a marker.
        long abstractMethodCount = sootClass.getMethods().stream()
                .filter(m -> !m.isStatic())
                .filter(SootMethod::isAbstract)
                .count();

        if (abstractMethodCount == 0) {
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "Interface declares no abstract methods (marker interface). " +
                    "Marker interfaces only tag a class with a type but provide no contract. " +
                    "Consider using a custom annotation instead, which is more flexible " +
                    "and doesn't affect the type hierarchy."
            ));
        }

        return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
    }
}
