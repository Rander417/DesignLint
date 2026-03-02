package designlint.fixtures;

/**
 * Test fixture: interface with multiple abstract methods. Should pass the
 * @FunctionalInterface check since it's NOT a SAM interface (can't be a lambda target).
 */
public interface MultiMethodInterface {
    void open();
    void close();
    boolean isOpen();
}
