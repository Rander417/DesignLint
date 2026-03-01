package samples;

/**
 * BAD: equals() doesn't check for null and skips instanceof.
 * This will throw NullPointerException when compared to null.
 * DesignLint should flag the missing null check and instanceof.
 */
public class BadEqualsPattern {
    private int x;
    private int y;

    public BadEqualsPattern(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        // BAD: no null check! Will throw NPE on: myObj.equals(null)
        // BAD: no instanceof check! Will throw ClassCastException on wrong types
        BadEqualsPattern other = (BadEqualsPattern) obj;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }
}
