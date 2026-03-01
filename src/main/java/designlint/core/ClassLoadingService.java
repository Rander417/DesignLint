package designlint.core;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles loading Java bytecodes via SootUp and categorizing classes.
 *
 * Per the requirements, loaded classes are separated into two categories:
 *   1. "Application classes" — explicitly chosen by the user
 *   2. "Dependency classes" — loaded because application classes depend on them
 *
 * Standard Java library classes (java.*, javax.*, jdk.*, etc.) are excluded
 * from both categories — they're never shown to the user.
 *
 * === SOOTUP CONCEPTS ===
 * - AnalysisInputLocation: tells SootUp where to find .class files (a directory, JAR, etc.)
 * - JavaView: the main entry point for accessing all loaded classes. Think of it as a
 *   "database" of all the bytecode SootUp has parsed.
 * - SootClass: SootUp's representation of a single Java class. From this you can inspect
 *   methods, fields, interfaces, superclass, etc.
 */
public class ClassLoadingService {

    /** Prefixes of classes we never want to show to the user. */
    private static final List<String> LIBRARY_PREFIXES = List.of(
            "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.", "org.xml."
    );

    private JavaView view;

    /** Class names the user explicitly selected for loading. */
    private final Set<String> userSelectedClassNames = new LinkedHashSet<>();

    /** All non-library classes discovered by SootUp. */
    private final Map<String, SootClass> loadedClasses = new LinkedHashMap<>();

    /**
     * Load classes from a given path (directory containing .class files, or a .jar).
     *
     * @param classPath            the path to load from
     * @param selectedClassNames   the fully qualified names of classes the user explicitly chose
     *                             (e.g., ["com.example.MyClass"]). Pass empty set to treat all as user-selected.
     * @return this service (for fluent chaining)
     */
    public ClassLoadingService load(Path classPath, Set<String> selectedClassNames) {
        userSelectedClassNames.clear();
        loadedClasses.clear();

        userSelectedClassNames.addAll(selectedClassNames);

        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(classPath, null);

        view = new JavaView(List.of(inputLocation));

        for (SootClass sootClass : view.getClasses()) {
            String fqn = sootClass.getType().getFullyQualifiedName();
            if (!isLibraryClass(fqn)) {
                loadedClasses.put(fqn, sootClass);
            }
        }

        if (userSelectedClassNames.isEmpty()) {
            userSelectedClassNames.addAll(loadedClasses.keySet());
        }

        return this;
    }

    /** Get the JavaView (needed by analysis checks that need to resolve types). */
    public JavaView getView() {
        return view;
    }

    /** Get classes explicitly selected by the user. */
    public List<SootClass> getUserSelectedClasses() {
        return loadedClasses.entrySet().stream()
                .filter(e -> userSelectedClassNames.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /** Get classes loaded as dependencies (not explicitly selected by the user). */
    public List<SootClass> getDependencyClasses() {
        return loadedClasses.entrySet().stream()
                .filter(e -> !userSelectedClassNames.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /** Get all non-library classes (both user-selected and dependencies). */
    public List<SootClass> getAllClasses() {
        return List.copyOf(loadedClasses.values());
    }

    /** Look up a single class by fully qualified name. */
    public Optional<SootClass> getClass(String fullyQualifiedName) {
        return Optional.ofNullable(loadedClasses.get(fullyQualifiedName));
    }

    /** Check if a class is a standard library class that should be hidden from the user. */
    private boolean isLibraryClass(String fullyQualifiedName) {
        return LIBRARY_PREFIXES.stream().anyMatch(fullyQualifiedName::startsWith);
    }

    /** Whether any classes have been loaded yet. */
    public boolean isLoaded() {
        return view != null && !loadedClasses.isEmpty();
    }

    /** Get the count of loaded application classes. */
    public int classCount() {
        return loadedClasses.size();
    }
}
