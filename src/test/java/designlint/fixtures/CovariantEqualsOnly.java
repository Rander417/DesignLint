package designlint.fixtures;

/**
 * Test fixture: defines equals(CovariantEqualsOnly) but NOT equals(Object).
 * This is the classic mistake — it OVERLOADS rather than OVERRIDES Object.equals().
 * HashSet, HashMap, etc. will completely ignore this method.
 */
public class CovariantEqualsOnly {
    private int id;

    // WRONG: This does NOT override Object.equals(Object)
    public boolean equals(CovariantEqualsOnly other) {
        if (other == null) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
