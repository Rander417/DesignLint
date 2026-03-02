package designlint.fixtures;

/**
 * Test fixture: catches Exception broadly. Should trigger a violation.
 * This is the "fishing net the size of a lake" anti-pattern.
 */
public class CatchesGenericException {
    private int value;

    public void doWork() {
        try {
            value = Integer.parseInt("42");
        } catch (Exception e) {
            // Catches EVERYTHING — NumberFormatException, NullPointerException,
            // ClassCastException... all of them silently handled the same way.
            System.err.println("Something went wrong: " + e.getMessage());
        }
    }
}
