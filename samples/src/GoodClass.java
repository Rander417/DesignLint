package samples;

/**
 * GOOD: This class follows all design guidelines correctly.
 * DesignLint should report all passes, no violations.
 *
 * - Does NOT implement Cloneable (uses copy constructor instead)
 * - Overrides BOTH equals() AND hashCode()
 * - equals() follows the recommended pattern: null check → instanceof → cast → compare
 */
public class GoodClass {
    private final int id;
    private final String name;

    public GoodClass(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Copy constructor (the recommended alternative to Cloneable)
    public GoodClass(GoodClass other) {
        this.id = other.id;
        this.name = other.name;
    }

    @Override
    public boolean equals(Object obj) {
        // Step 1: null check
        if (obj == null) return false;
        // Step 2: instanceof type check
        if (!(obj instanceof GoodClass)) return false;
        // Step 3: cast (same type as instanceof)
        GoodClass other = (GoodClass) obj;
        // Step 4: actual comparison
        return this.id == other.id
            && (this.name == null ? other.name == null : this.name.equals(other.name));
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
