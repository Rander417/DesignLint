package designlint.fixtures;

/**
 * Test fixture: contains a method with an empty catch block.
 * The exception is caught and completely ignored — the worst pattern.
 */
public class EmptyCatchBlock {
    private int value;

    public void riskyOperation() {
        try {
            value = Integer.parseInt("not a number");
        } catch (NumberFormatException e) {
            // Deliberately empty — this is the anti-pattern we're detecting
        }
    }
}
