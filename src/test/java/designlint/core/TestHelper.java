package designlint.core;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.util.List;
import java.util.Optional;

/**
 * Test utility that loads compiled test fixture classes via SootUp.
 *
 * === HOW THIS WORKS ===
 * When Gradle compiles tests, the fixture classes (in designlint.fixtures package)
 * get compiled to .class files in the test output directory. We point SootUp at
 * that directory using JavaClassPathAnalysisInputLocation — the same input type
 * you'd use for any classpath-based analysis.
 *
 * This approach means our test fixtures are real compiled Java bytecode,
 * not mocked data, so we're testing against exactly what DesignLint would
 * see in the real world.
 */
public class TestHelper {

    private final JavaView view;

    public TestHelper() {
        // "build/classes/java/test" is where Gradle puts compiled test classes.
        // JavaClassPathAnalysisInputLocation treats it like a classpath entry.
        AnalysisInputLocation inputLocation =
                new JavaClassPathAnalysisInputLocation("build/classes/java/test");

        view = new JavaView(List.of(inputLocation));
    }

    /**
     * Load a fixture class by its fully qualified name.
     *
     * @param fullyQualifiedName e.g., "designlint.fixtures.ImplementsCloneable"
     * @return the SootClass, or throws if not found
     */
    public SootClass getFixtureClass(String fullyQualifiedName) {
        ClassType classType = view.getIdentifierFactory().getClassType(fullyQualifiedName);
        Optional<SootClass> result = (Optional<SootClass>) (Optional<?>) view.getClass(classType);
        return result.orElseThrow(() ->
                new RuntimeException("Fixture class not found: " + fullyQualifiedName +
                        ". Make sure the test is compiled (run: gradlew testClasses)")
        );
    }

    /**
     * Shorthand: load a fixture class from the designlint.fixtures package.
     *
     * @param simpleClassName e.g., "ImplementsCloneable"
     * @return the SootClass
     */
    public SootClass fixture(String simpleClassName) {
        return getFixtureClass("designlint.fixtures." + simpleClassName);
    }

    /** Get the JavaView for checks that need it. */
    public JavaView getView() {
        return view;
    }
}
