package designlint.fixtures;

import java.io.IOException;

/**
 * Test fixture: method declares a specific exception type. Should pass.
 * The caller knows exactly what can go wrong.
 */
public class ThrowsSpecificException {

    public void readFile(String path) throws IOException {
        if (path == null) {
            throw new IOException("Path cannot be null");
        }
        // Caller knows: this method does I/O and it might fail
    }
}
