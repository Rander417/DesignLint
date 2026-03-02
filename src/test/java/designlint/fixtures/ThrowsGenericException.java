package designlint.fixtures;

/**
 * Test fixture: method declares "throws Exception". Should trigger a violation.
 * Forces every caller to deal with the vaguest possible exception contract.
 */
public class ThrowsGenericException {

    public void processData(String input) throws Exception {
        if (input == null) {
            throw new Exception("Input was null");
        }
        // In reality, this should throw a specific type like IllegalArgumentException
    }
}
