package designlint.fixtures;

/**
 * Test fixture: does NOT implement Cloneable. Should pass.
 * Uses a copy constructor instead (the recommended approach).
 */
public class NoCloneable {
    private int value;

    public NoCloneable(int value) {
        this.value = value;
    }

    // Copy constructor — the right way to do it
    public NoCloneable(NoCloneable other) {
        this.value = other.value;
    }
}
