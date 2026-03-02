package designlint.fixtures;

import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture: non-final static fields. Should trigger violations.
 */
public class MutableStaticFields {
    public static int counter = 0;
    public static List<String> registry = new ArrayList<>();
}
