package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Design Guideline Check: Catch-All Exception Handler
 *
 * WHY THIS MATTERS:
 * Catching Exception or Throwable is like using a fishing net the size of a lake.
 * You'll catch the fish you wanted, but also every boot, tire, and shopping cart
 * in the water. Specifically, you'll silently swallow:
 *
 *   - NullPointerException (bug in your code)
 *   - OutOfMemoryError (if catching Throwable)
 *   - ClassCastException (another bug)
 *   - StackOverflowError (yet another bug)
 *
 * The whole point of Java's checked exception system is to force you to think
 * about what can go wrong. Catching Exception defeats that entirely.
 *
 * WHAT WE CHECK:
 * SootUp 1.3.0 reports ALL @caughtexception entries as java.lang.Throwable,
 * regardless of what the source code actually catches. The real exception type
 * appears in the cast statements that follow. For example:
 *
 *   catch (NumberFormatException e) { ... }
 *
 * becomes in Jimple:
 *   $stack3 := @caughtexception             // always typed as Throwable
 *   e = (java.lang.Throwable) $stack3       // generic cast (bookkeeping)
 *   #l0 = (java.lang.NumberFormatException) $stack3  // THE REAL TYPE
 *
 * So we scan the cast expressions after each @caughtexception to find the most
 * specific type the exception is cast to. If the most specific cast is still
 * java.lang.Exception or java.lang.Throwable, it's a catch-all.
 */
public class CatchAllCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Catch-All Exception Check";

    private static final Set<String> OVERLY_BROAD_TYPES = Set.of(
            "java.lang.Exception",
            "java.lang.Throwable"
    );

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public Severity severity() {
        return Severity.WARNING;
    }

    @Override
    public String description() {
        return "Warns if a method catches Exception or Throwable broadly, " +
               "which swallows all exceptions including bugs like NullPointerException.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();
        List<AnalysisResult> violations = new ArrayList<>();

        for (SootMethod method : sootClass.getMethods()) {
            if (method.isAbstract() || method.isNative()) {
                continue;
            }

            try {
                Body body = method.getBody();
                List<Stmt> stmts = body.getStmtGraph().getStmts();

                for (int i = 0; i < stmts.size(); i++) {
                    Stmt stmt = stmts.get(i);

                    if (stmt instanceof JIdentityStmt identityStmt
                            && identityStmt.getRightOp() instanceof JCaughtExceptionRef) {

                        // Found a catch handler. Now look at the cast statements
                        // that follow to find the actual caught exception type.
                        String actualCaughtType = findActualCaughtType(stmts, i + 1);

                        if (actualCaughtType != null && OVERLY_BROAD_TYPES.contains(actualCaughtType)) {
                            violations.add(new AnalysisResult.Violation(
                                    className,
                                    GUIDELINE_NAME,
                                    severity(),
                                    "Method " + method.getName() + "() catches " + actualCaughtType +
                                    ". This is too broad — it will swallow bugs like " +
                                    "NullPointerException and ClassCastException. " +
                                    "Catch specific exception types instead."
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                // Skip methods whose bodies can't be parsed
            }
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }

    /**
     * Scan forward from a caught exception to find what type it's actually cast to.
     *
     * SootUp always types @caughtexception as Throwable. The actual catch type
     * appears as a cast expression referencing the caught variable. We look for
     * the most specific (non-Throwable) cast type within the next several statements.
     *
     * Example Jimple for catch(NumberFormatException e):
     *   $stack3 := @caughtexception
     *   e = (java.lang.Throwable) $stack3         // generic — skip this
     *   #l0 = (java.lang.NumberFormatException) $stack3  // this is the real type
     *
     * Returns the most specific caught type found, or "java.lang.Throwable" if
     * no more specific cast is present.
     */
    private String findActualCaughtType(List<Stmt> stmts, int startIndex) {
        String mostSpecificType = "java.lang.Throwable"; // default

        // Scan a window of statements after the catch entry
        for (int j = startIndex; j < Math.min(startIndex + 8, stmts.size()); j++) {
            Stmt s = stmts.get(j);

            if (s instanceof JAssignStmt assignStmt) {
                Value rightOp = assignStmt.getRightOp();

                // Look for cast expressions: localVar = (SomeType) $caughtVar
                if (rightOp instanceof JCastExpr castExpr) {
                    String castType = castExpr.getType().toString();

                    // Track the most specific (non-Throwable) type we see
                    if (!castType.equals("java.lang.Throwable")) {
                        mostSpecificType = castType;
                    }
                }
            }
        }

        return mostSpecificType;
    }
}
