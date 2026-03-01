package designlint.fixtures;

/**
 * Test fixture: a plain class with no overrides and no Cloneable.
 * Should pass every single check — the cleanest possible class.
 */
public class PlainClass {
    private String data;

    public PlainClass(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
