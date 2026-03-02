package designlint.fixtures;

import java.util.List;

/**
 * Test fixture: only final static fields (constants). Should pass.
 */
public class FinalStaticFields {
    public static final int MAX_SIZE = 100;
    public static final String APP_NAME = "DesignLint";
    public static final List<String> EMPTY = List.of();
}
