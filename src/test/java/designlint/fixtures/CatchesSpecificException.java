package designlint.fixtures;

/**
 * Test fixture: catches a specific exception type. Should pass.
 * This is the right way to do it.
 */
public class CatchesSpecificException {
    private int value;

    public void doWork() {
        try {
            value = Integer.parseInt("not a number");
        } catch (NumberFormatException e) {
            // Good: catches exactly what can go wrong and nothing else
            value = -1;
            System.err.println("Bad number format: " + e.getMessage());
        }
    }
}
