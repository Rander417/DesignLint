package designlint.fixtures;

/**
 * Test fixture: public non-final instance fields. Should trigger violations.
 * Classic "struct-style" Java — breaks encapsulation.
 */
public class PublicMutableFields {
    public String name;
    public int age;
    public double salary;
}
