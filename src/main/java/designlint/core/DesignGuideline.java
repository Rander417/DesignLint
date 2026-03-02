package designlint.core;

import sootup.core.model.SootClass;
import sootup.java.core.views.JavaView;

import java.util.List;

/**
 * The contract that every design guideline check must implement.
 *
 * This is the "plugin interface" — each concrete check (Cloneable, equals/hashCode, etc.)
 * implements this interface. The modular architecture means:
 *   - New checks are added by creating a new class that implements this interface
 *   - No existing code needs to change
 *   - Checks can be enabled/disabled independently by the user
 *
 * === DESIGN PATTERN NOTE ===
 * This is the Strategy pattern. The AnalysisEngine doesn't know or care what specific
 * checks exist — it just calls analyze() on whatever DesignGuideline instances it's given.
 */
public interface DesignGuideline {

    /**
     * A short, unique name for this guideline (shown in the UI).
     * Example: "Cloneable Check", "equals/hashCode Check"
     */
    String name();

    /**
     * A longer description of what this check looks for.
     * Displayed in the UI to help users understand what they're enabling.
     */
    String description();

    /**
     * The default severity level for violations produced by this guideline.
     *
     *   ERROR    — Will cause bugs (e.g., broken equals/hashCode contract)
     *   WARNING  — Creates serious risk (e.g., deprecated APIs, exception swallowing)
     *   ADVISORY — Best practice recommendation (e.g., missing toString())
     *
     * Most guidelines return a single severity for all their violations.
     * Guidelines that produce violations of varying severity (e.g., CovariantEquals)
     * can override per-violation by passing a different Severity to the Violation record.
     */
    Severity severity();

    /**
     * Analyze a single class against this design guideline.
     *
     * @param sootClass the SootUp class representation to analyze
     * @param view      the JavaView context (needed for resolving types, looking up
     *                  related classes, etc.)
     * @return a list of results — could be a single Pass, a single Violation,
     *         or multiple Violations if the check finds several issues in one class
     */
    List<AnalysisResult> analyze(SootClass sootClass, JavaView view);
}
