package designlint.fixtures;

/**
 * Test fixture: the worst offender — violates ALL three guidelines.
 * - Implements Cloneable
 * - Has equals() without hashCode()
 * - equals() pattern is missing null check and instanceof
 */
public class AllViolations implements Cloneable {
    private int id;

    @Override
    public boolean equals(Object obj) {
        // BAD: no null check, no instanceof
        AllViolations other = (AllViolations) obj;
        return this.id == other.id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
