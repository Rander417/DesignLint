package designlint.fixtures;

/**
 * Test fixture: a normal interface with abstract methods. Should pass the empty check.
 * This is also a SAM interface (one abstract method), which we'll use for
 * the @FunctionalInterface check too — it's missing the annotation.
 */
public interface NonEmptyInterface {
    void doSomething(String input);
}
