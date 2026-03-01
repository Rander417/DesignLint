package designlint.fixtures;

import java.io.Serializable;

/**
 * Test fixture: implements Serializable WITH a proper serialVersionUID. Should pass.
 */
public class SerializableWithUID implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int value;
}
