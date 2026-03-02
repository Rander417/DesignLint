package designlint.fixtures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture: public methods that return collection interfaces. Should pass.
 * The internal implementation uses concrete types (fine), but the public API
 * exposes only the interface — callers don't know or care about the impl.
 */
public class ReturnsInterfaceCollection {
    private List<String> items = new ArrayList<>();
    private Map<String, Integer> counts = new HashMap<>();

    public List<String> getItems() {
        return items;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }
}
