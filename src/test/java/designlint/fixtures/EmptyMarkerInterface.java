package designlint.fixtures;

/**
 * Test fixture: an interface with no methods — a custom marker interface.
 * Should trigger a violation. Use an annotation instead.
 */
public interface EmptyMarkerInterface {
    // No methods — this is just a tag
}
