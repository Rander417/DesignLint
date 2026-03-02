package designlint.fixtures;

/**
 * Test fixture: all-static-methods class with default public constructor.
 * Should trigger a violation — this class shouldn't be instantiable.
 */
public class UtilityClassPublicCtor {
    // No explicit constructor → Java provides public default

    public static int add(int a, int b) {
        return a + b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    public static String repeat(String s, int times) {
        return s.repeat(times);
    }
}
