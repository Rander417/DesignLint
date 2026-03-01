package designlint.fixtures;

/**
 * Test fixture: correctly overrides BOTH equals() and hashCode(). Should pass.
 */
public class BothEqualsAndHashCode {
    private int id;
    private String name;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof BothEqualsAndHashCode)) return false;
        BothEqualsAndHashCode other = (BothEqualsAndHashCode) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return id * 31;
    }
}
