package designlint.fixtures;

/**
 * Test fixture: has fields AND overrides toString(). Should pass.
 */
public class HasToString {
    private int id;
    private String name;

    @Override
    public String toString() {
        return "HasToString{id=" + id + ", name='" + name + "'}";
    }
}
