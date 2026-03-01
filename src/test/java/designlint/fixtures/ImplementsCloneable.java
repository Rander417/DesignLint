package designlint.fixtures;

/**
 * Test fixture: implements Cloneable. Should trigger a violation.
 */
public class ImplementsCloneable implements Cloneable {
    private int value;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
