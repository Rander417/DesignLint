package designlint.fixtures;

/**
 * Test fixture: has BOTH equals(Object) and equals(CovariantEqualsWithProper).
 * The proper override exists so it's not broken, but the covariant overload
 * is a code smell — it can cause confusion about which method gets called.
 */
public class CovariantEqualsWithProper {
    private int id;

    // Proper override — this is the one collections use
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof CovariantEqualsWithProper)) return false;
        return this.id == ((CovariantEqualsWithProper) obj).id;
    }

    // Covariant convenience overload — technically works but smells
    public boolean equals(CovariantEqualsWithProper other) {
        if (other == null) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
