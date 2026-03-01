package designlint.fixtures;

/**
 * Test fixture: equals() does a raw cast with no null or instanceof check.
 * Should trigger violations for both missing null check and missing instanceof.
 */
public class EqualsRawCast {
    private int x;

    @Override
    public boolean equals(Object obj) {
        // BAD: no null check, no instanceof — just raw cast
        EqualsRawCast other = (EqualsRawCast) obj;
        return this.x == other.x;
    }

    @Override
    public int hashCode() {
        return x;
    }
}
