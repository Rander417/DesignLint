package designlint.fixtures;

/**
 * Test fixture: single abstract method interface WITH @FunctionalInterface. Should pass.
 * The annotation is present, documenting intent and providing compile-time safety.
 */
@FunctionalInterface
public interface AnnotatedFunctionalInterface {
    boolean validate(String input);
}
