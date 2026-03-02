package designlint.fixtures;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Test fixture: public methods that return concrete collection types.
 * Should trigger violations for both methods.
 */
public class ReturnsConcreteCollection {
    private ArrayList<String> items = new ArrayList<>();
    private HashMap<String, Integer> counts = new HashMap<>();

    public ArrayList<String> getItems() {
        return items;
    }

    public HashMap<String, Integer> getCounts() {
        return counts;
    }
}
