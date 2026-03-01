package designlint.fixtures;

import java.io.Serializable;

/**
 * Test fixture: implements Serializable but MISSING serialVersionUID. Should trigger violation.
 * This is the classic "2am production incident" setup — any class change breaks deserialization.
 */
public class SerializableWithoutUID implements Serializable {
    private String name;
    private int value;
}
