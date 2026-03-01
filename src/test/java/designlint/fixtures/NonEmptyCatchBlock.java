package designlint.fixtures;

/**
 * Test fixture: contains a catch block that actually does something. Should pass.
 */
public class NonEmptyCatchBlock {
    private int value;

    public void riskyOperation() {
        try {
            value = Integer.parseInt("not a number");
        } catch (NumberFormatException e) {
            // This catch block is NOT empty — it handles the exception
            value = -1;
            System.err.println("Parse failed: " + e.getMessage());
        }
    }
}
