package designlint.fixtures;

/**
 * Test fixture: overrides hashCode() but NOT equals(). Should trigger a violation.
 */
public class HashCodeWithoutEquals {
    private int id;

    @Override
    public int hashCode() {
        return id * 31;
    }
}
