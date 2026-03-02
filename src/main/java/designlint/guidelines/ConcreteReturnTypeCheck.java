package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Design Guideline Check: Concrete Collection Return Type
 *
 * WHY THIS MATTERS:
 * Returning ArrayList instead of List (or HashMap instead of Map) from a public
 * method locks every caller into a specific implementation. If you later realize
 * LinkedList or TreeMap would be better, you have to change every caller.
 *
 * "Program to an interface, not an implementation" is one of the foundational
 * principles from the Gang of Four. For collection types in Java, this means:
 *   - Return List, not ArrayList/LinkedList
 *   - Return Map, not HashMap/TreeMap
 *   - Return Set, not HashSet/TreeSet
 *
 * WHAT WE CHECK:
 * We examine the return type of each public method. If it's a concrete collection
 * class from java.util, we flag it and suggest the appropriate interface.
 * We only check public methods since private/package-private methods are
 * implementation details where concrete types are more acceptable.
 */
public class ConcreteReturnTypeCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Concrete Return Type Check";

    /**
     * Maps concrete collection types to their preferred interface equivalents.
     * When we find a method returning a key, we suggest the value instead.
     */
    private static final Map<String, String> CONCRETE_TO_INTERFACE = Map.ofEntries(
            // List implementations
            Map.entry("java.util.ArrayList", "java.util.List"),
            Map.entry("java.util.LinkedList", "java.util.List"),
            Map.entry("java.util.Vector", "java.util.List"),
            Map.entry("java.util.Stack", "java.util.Deque"),
            Map.entry("java.util.CopyOnWriteArrayList", "java.util.List"),
            // Set implementations
            Map.entry("java.util.HashSet", "java.util.Set"),
            Map.entry("java.util.LinkedHashSet", "java.util.Set"),
            Map.entry("java.util.TreeSet", "java.util.SortedSet or java.util.Set"),
            // Map implementations
            Map.entry("java.util.HashMap", "java.util.Map"),
            Map.entry("java.util.LinkedHashMap", "java.util.Map"),
            Map.entry("java.util.TreeMap", "java.util.SortedMap or java.util.Map"),
            Map.entry("java.util.Hashtable", "java.util.Map"),
            Map.entry("java.util.ConcurrentHashMap", "java.util.Map or ConcurrentMap"),
            // Queue/Deque implementations
            Map.entry("java.util.ArrayDeque", "java.util.Deque"),
            Map.entry("java.util.PriorityQueue", "java.util.Queue")
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
        return "Warns if public methods return concrete collection types (ArrayList, HashMap, etc.) " +
               "instead of interfaces (List, Map, etc.). Program to interfaces, not implementations.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();
        List<AnalysisResult> violations = new ArrayList<>();

        for (SootMethod method : sootClass.getMethods()) {
            // Only check public methods — private methods are implementation details
            if (!method.isPublic()) {
                continue;
            }

            // Skip constructors and static initializers
            if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) {
                continue;
            }

            Type returnType = method.getReturnType();

            // We only care about class types (not primitives, void, arrays)
            if (returnType instanceof ClassType classReturnType) {
                String returnTypeName = classReturnType.getFullyQualifiedName();
                String suggestedInterface = CONCRETE_TO_INTERFACE.get(returnTypeName);

                if (suggestedInterface != null) {
                    violations.add(new AnalysisResult.Violation(
                            className,
                            GUIDELINE_NAME,
                            severity(),
                            "Method " + method.getName() + "() returns " +
                            classReturnType.getClassName() + " (concrete type). " +
                            "Consider returning " + suggestedInterface + " instead. " +
                            "This decouples callers from the specific implementation " +
                            "and makes future changes easier."
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
