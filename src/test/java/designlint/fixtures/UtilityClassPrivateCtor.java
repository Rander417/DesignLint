package designlint.fixtures;

/**
 * Test fixture: all-static-methods class with private constructor. Should pass.
 * This is the correct pattern for utility classes.
 */
public class UtilityClassPrivateCtor {
    private UtilityClassPrivateCtor() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }
}
