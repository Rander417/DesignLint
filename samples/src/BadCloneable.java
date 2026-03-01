package samples;

/**
 * BAD: Implements Cloneable. DesignLint should flag this.
 */
public class BadCloneable implements Cloneable {
    private String name;
    private int value;

    public BadCloneable(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // GOOD alternative: copy constructor (what DesignLint recommends)
    // public BadCloneable(BadCloneable other) {
    //     this.name = other.name;
    //     this.value = other.value;
    // }
}
