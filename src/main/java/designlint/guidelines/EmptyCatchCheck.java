package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Guideline Check #8: Empty Catch Block Detection
 *
 * WHY THIS MATTERS:
 * An empty catch block silently swallows an exception and pretends nothing happened.
 * This is the programming equivalent of a check engine light duct-taped over.
 * The program continues in an unknown state, bugs become invisible, and when things
 * finally do crash, the original cause is long gone from the stack trace.
 *
 * Even if you genuinely want to ignore an exception (rare but sometimes valid),
 * best practice is to at least log it or add a comment explaining why.
 *
 * WHAT WE CHECK:
 * We walk the Jimple IR of each method body looking for caught exception handlers
 * (JIdentityStmt with JCaughtExceptionRef). In Jimple, a catch block starts with:
 *
 *   $r = @caughtexception   // identity stmt catching the exception
 *
 * If the very next statement after this is a goto or return (meaning the catch block
 * does absolutely nothing with the exception), we flag it as a violation.
 *
 * NOTE: This is a heuristic. Some compiler optimizations could produce false positives
 * or negatives, but in practice this catches the vast majority of empty catch blocks.
 */
public class EmptyCatchCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "Empty Catch Block Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public String description() {
        return "Warns if any method contains an empty catch block that silently " +
               "swallows exceptions without logging or handling them.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();
        List<AnalysisResult> violations = new ArrayList<>();

        for (SootMethod method : sootClass.getMethods()) {
            // Skip abstract/native methods (no body to analyze)
            if (method.isAbstract() || method.isNative()) {
                continue;
            }

            try {
                Body body = method.getBody();
                List<Stmt> stmts = body.getStmtGraph().getStmts();

                // Scan for caught exception identity statements
                for (int i = 0; i < stmts.size(); i++) {
                    Stmt stmt = stmts.get(i);

                    if (isCaughtExceptionStmt(stmt)) {
                        // Found a catch handler entry point.
                        // The compiler typically generates:
                        //   $stackN := @caughtexception   // identity (we're here)
                        //   localVar = $stackN            // store to named local
                        //   goto labelX                   // exit catch block
                        //
                        // So an "empty" catch may have 1-2 trivial statements
                        // (just storing the exception reference) before the exit.
                        // We look ahead up to 3 statements for a goto/return,
                        // counting only identity and assignment stmts as trivial.
                        if (isCatchBlockEmpty(stmts, i + 1)) {
                            violations.add(new AnalysisResult.Violation(
                                    className,
                                    GUIDELINE_NAME,
                                    "Method " + method.getName() + "() contains an empty catch block. " +
                                    "Silently swallowing exceptions hides bugs and makes debugging " +
                                    "extremely difficult. At minimum, log the exception."
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                // If we can't parse the method body, skip it gracefully.
                // This can happen with synthetic or compiler-generated methods.
            }
        }

        if (violations.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return violations;
    }

    /**
     * Check if a statement is a caught exception identity statement.
     * In Jimple: $r = @caughtexception
     */
    private boolean isCaughtExceptionStmt(Stmt stmt) {
        if (stmt instanceof JIdentityStmt identityStmt) {
            return identityStmt.getRightOp() instanceof JCaughtExceptionRef;
        }
        return false;
    }

    /**
     * Determine if the catch block starting at the given index is empty.
     *
     * After the @caughtexception identity stmt, the compiler may generate
     * 1-2 trivial statements (storing the exception to a local variable)
     * before the actual catch body begins. We look ahead up to 3 statements:
     * if all we find before a goto/return are assignments and identity stmts
     * (i.e., no method calls, no field writes, no real logic), the catch is empty.
     */
    private boolean isCatchBlockEmpty(List<Stmt> stmts, int startIndex) {
        for (int j = startIndex; j < Math.min(startIndex + 3, stmts.size()); j++) {
            Stmt s = stmts.get(j);

            if (isExitStatement(s)) {
                // Reached a goto/return with only trivial stmts in between → empty
                return true;
            }

            // Allow trivial statements: local variable assignments that just
            // shuffle the exception reference around
            if (s instanceof JAssignStmt || s instanceof JIdentityStmt) {
                continue;
            }

            // Any other kind of statement means the catch block does real work
            return false;
        }
        return false;
    }

    /**
     * Check if a statement is an "exit" from a catch block — a goto or return.
     * If this immediately follows a caught exception identity stmt, the catch is empty.
     */
    private boolean isExitStatement(Stmt stmt) {
        return stmt instanceof JGotoStmt
                || stmt instanceof JReturnStmt
                || stmt instanceof JReturnVoidStmt;
    }
}



