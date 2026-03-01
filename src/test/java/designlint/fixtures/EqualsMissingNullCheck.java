package designlint.fixtures;

/**
 * Test fixture: equals() skips the null check. Should trigger a violation.
 * Will throw NullPointerException when: obj.equals(null)
 */
public class EqualsMissingNullCheck {
    private int x;

    @Override
    public boolean equals(Object obj) {
        // BAD: no null check
        if (!(obj instanceof EqualsMissingNullCheck)) return false;
        EqualsMissingNullCheck other = (EqualsMissingNullCheck) obj;
        return this.x == other.x;
    }

    @Override
    public int hashCode() {
        return x;
    }
}
