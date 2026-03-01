package designlint.fixtures;

/**
 * Test fixture: overrides equals() but NOT hashCode(). Should trigger a violation.
 */
public class EqualsWithoutHashCode {
    private int id;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof EqualsWithoutHashCode)) return false;
        return this.id == ((EqualsWithoutHashCode) obj).id;
    }
}
