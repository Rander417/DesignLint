package designlint.fixtures;

/**
 * Test fixture: implements Comparable AND overrides equals(). Should pass.
 * compareTo and equals are consistent — both compare by 'value'.
 */
public class ComparableWithEquals implements Comparable<ComparableWithEquals> {
    private int value;

    @Override
    public int compareTo(ComparableWithEquals other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof ComparableWithEquals)) return false;
        return this.value == ((ComparableWithEquals) obj).value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
