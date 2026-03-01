package designlint.fixtures;

/**
 * Test fixture: equals() follows the full recommended pattern. Should pass.
 */
public class GoodEqualsPattern {
    private int x;
    private int y;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;                        // Step 1: null check
        if (!(obj instanceof GoodEqualsPattern)) return false; // Step 2: instanceof
        GoodEqualsPattern other = (GoodEqualsPattern) obj;     // Step 3: cast
        return this.x == other.x && this.y == other.y;         // Step 4: comparison
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }
}
