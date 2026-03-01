package designlint.fixtures;

/**
 * Test fixture: overrides NEITHER equals() nor hashCode(). Should pass.
 * Inheriting both from Object is perfectly fine — the contract is maintained.
 */
public class NeitherEqualsNorHashCode {
    private int id;
    private String name;

    public NeitherEqualsNorHashCode(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
