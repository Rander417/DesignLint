package designlint.core;

/**
 * Severity levels for design guideline violations.
 *
 * The three tiers reflect how directly the issue impacts program correctness:
 *
 *   ERROR    — Will cause bugs. Your program will behave incorrectly, potentially
 *              silently. Think: HashMap broken because equals/hashCode contract
 *              is violated.
 *
 *   WARNING  — Won't break your code today, but creates serious risk. Deprecated
 *              APIs, exception-swallowing patterns, concurrency hazards, contract
 *              violations that bite in specific contexts.
 *
 *   ADVISORY — Code quality and best practice. Your code works, but it's harder
 *              to maintain, debug, or extend. Professional recommendations that
 *              you should act on, but your program won't crash if you don't.
 */
public enum Severity {
    ERROR("Error"),
    WARNING("Warning"),
    ADVISORY("Advisory");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    /** Human-friendly label for GUI and reports. */
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
