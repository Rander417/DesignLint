package samples;

/**
 * BAD: Overrides equals() but NOT hashCode(). DesignLint should flag this.
 * Putting this in a HashSet or HashMap will produce incorrect behavior.
 */
public class BadEqualsOnly {
    private int id;
    private String name;

    public BadEqualsOnly(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof BadEqualsOnly)) return false;
        BadEqualsOnly other = (BadEqualsOnly) obj;
        return this.id == other.id;
    }

    // Missing hashCode()! This breaks the equals/hashCode contract.
}
