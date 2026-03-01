package designlint.fixtures;

/**
 * Test fixture: implements Comparable but does NOT override equals().
 * This means compareTo defines ordering but equals uses Object's identity check.
 * TreeSet and HashSet will disagree about what's a "duplicate."
 */
public class ComparableWithoutEquals implements Comparable<ComparableWithoutEquals> {
    private int priority;
    private String label;

    @Override
    public int compareTo(ComparableWithoutEquals other) {
        return Integer.compare(this.priority, other.priority);
    }

    // Note: no equals() override — uses Object.equals() (identity comparison)
    // This is inconsistent with compareTo which compares by priority
}
