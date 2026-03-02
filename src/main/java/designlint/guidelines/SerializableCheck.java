package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootField;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * Design Guideline Check #4: Serializable without serialVersionUID
 *
 * WHY THIS MATTERS:
 * When a class implements java.io.Serializable, the JVM uses a serialVersionUID to
 * verify that a serialized object and the class definition are compatible during
 * deserialization. If you don't declare one explicitly, the JVM auto-generates it
 * based on the class structure (fields, methods, etc.). Change ANYTHING — add a
 * method, reorder fields, even add a blank line that shifts compilation — and the
 * auto-generated UID changes. Your previously serialized objects become unreadable.
 *
 * This is one of those bugs that doesn't show up until production, when you deploy
 * a new version and all your cached/persisted objects explode with
 * InvalidClassException. Ask anyone who's been on-call for a Java service at 2am.
 *
 * WHAT WE CHECK:
 * 1. Does the class implement java.io.Serializable?
 * 2. If yes, does it declare a field: static final long serialVersionUID?
 * 3. If not → violation
 *
 * See Effective Java, Item 87: "Consider using a custom serialized form."
 */
public class SerializableCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Serializable Check";

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
        return "Warns if a class implements Serializable without declaring a " +
               "serialVersionUID field. Missing this causes fragile deserialization.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // Check if the class implements java.io.Serializable
        boolean implementsSerializable = sootClass.getInterfaces().stream()
                .map(ClassType::getFullyQualifiedName)
                .anyMatch(name -> name.equals("java.io.Serializable"));

        if (!implementsSerializable) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        // Class is Serializable — now check for serialVersionUID field.
        // It must be: static final long serialVersionUID
        boolean hasSerialVersionUID = sootClass.getFields().stream()
                .anyMatch(this::isSerialVersionUID);

        if (hasSerialVersionUID) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return List.of(new AnalysisResult.Violation(
                className,
                GUIDELINE_NAME,
                severity(),
                "Class implements Serializable but does not declare a serialVersionUID. " +
                "Without an explicit serialVersionUID, any change to the class structure " +
                "will break deserialization of previously serialized objects. " +
                "Add: private static final long serialVersionUID = 1L;"
        ));
    }

    /**
     * Check if a field matches the serialVersionUID signature.
     * Must be named "serialVersionUID", static, and of type long.
     * We don't enforce 'final' strictly because some codebases omit it,
     * but the field must exist and be static long.
     */
    private boolean isSerialVersionUID(SootField field) {
        return field.getName().equals("serialVersionUID")
                && field.isStatic()
                && field.getType().toString().equals("long");
    }
}
