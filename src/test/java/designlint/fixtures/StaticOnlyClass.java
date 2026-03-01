package designlint.fixtures;

/**
 * Test fixture: only has static fields, no instance fields.
 * Should PASS the toString check — there's nothing meaningful to print
 * for an instance of a utility/constants class.
 */
public class StaticOnlyClass {
    public static final int MAX_RETRIES = 3;
    public static final String DEFAULT_NAME = "unknown";

    public static int computeSomething(int x) {
        return x * MAX_RETRIES;
    }
}
