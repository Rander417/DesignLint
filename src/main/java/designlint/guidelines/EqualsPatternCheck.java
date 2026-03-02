package designlint.guidelines;

import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Design Guideline Check #3: equals() Method Implementation Pattern
 *
 * From the requirements, if a class overrides equals(Object), the implementation
 * should follow this pattern:
 *
 *   (a) Check if argument is null → return false
 *       (equals must never throw NullPointerException)
 *   (b) Check if argument type is compatible using instanceof → return false if not
 *   (c) Cast the argument to the comparable type (same type used in instanceof)
 *   (d) Arbitrary code for the actual comparison
 *
 * === HOW THIS WORKS ===
 * SootUp converts Java bytecode into Jimple, a simplified 3-address intermediate
 * representation. Instead of working with complex bytecode instructions, we work
 * with clean statements like:
 *
 *   r1 := @parameter0: java.lang.Object    // the 'other' parameter
 *   if r1 == null goto label1               // null check
 *   $z0 = r1 instanceof MyClass            // type check
 *   if $z0 == 0 goto label2                // branch on instanceof result
 *   r2 = (MyClass) r1                      // cast
 *   ... actual comparison logic ...
 *
 * We walk through these statements looking for the required elements in order.
 * We're pragmatic rather than rigid — bytecodes from different compilers produce
 * slightly different Jimple, so we check for the PRESENCE and ORDER of key
 * elements rather than demanding an exact statement sequence.
 */
public class EqualsPatternCheck implements DesignGuideline {

    private static final String GUIDELINE_NAME = "equals() Pattern Check";

    @Override
    public String name() {
        return GUIDELINE_NAME;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "Checks that the equals() method follows the recommended pattern: " +
               "null check, instanceof type check, cast, then comparison logic.";
    }

    @Override
    public List<AnalysisResult> analyze(SootClass sootClass, JavaView view) {
        String className = sootClass.getType().getFullyQualifiedName();

        // First, find the equals(Object) method
        Optional<? extends SootMethod> equalsMethod = findEqualsMethod(sootClass);

        // If the class doesn't override equals(), this check doesn't apply
        if (equalsMethod.isEmpty()) {
            return List.of(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        SootMethod method = equalsMethod.get();

        // Try to get the method body (Jimple representation)
        Body body;
        try {
            body = method.getBody();
        } catch (Exception e) {
            // SootUp can sometimes fail to parse complex bytecode.
            // We report this as a soft warning rather than crashing.
            return List.of(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "Could not analyze equals() method body: " + e.getMessage()
            ));
        }

        // Walk the Jimple statements and check for the required pattern elements
        return checkEqualsPattern(className, body);
    }

    /**
     * Find the equals(Object) method in the class.
     * Returns empty if the class doesn't override equals.
     */
    private Optional<? extends SootMethod> findEqualsMethod(SootClass sootClass) {
        return sootClass.getMethods().stream()
                .filter(m -> m.getName().equals("equals"))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> m.getParameterTypes().get(0).toString().equals("java.lang.Object"))
                .findFirst();
    }

    /**
     * Walk the Jimple statements and verify the equals() pattern.
     *
     * We look for three key elements in order:
     * 1. A null comparison (comparing parameter against null)
     * 2. An instanceof check
     * 3. A cast to the same type used in the instanceof
     */
    private List<AnalysisResult> checkEqualsPattern(String className, Body body) {
        List<AnalysisResult> results = new ArrayList<>();
        StmtGraph<?> stmtGraph = body.getStmtGraph();
        List<Stmt> stmts = stmtGraph.getStmts();

        boolean foundNullCheck = false;
        boolean foundInstanceOf = false;
        boolean foundCast = false;
        String instanceOfType = null;  // track the type used in instanceof
        String castType = null;        // track the type used in cast

        // Walk each statement in order, looking for our pattern elements
        for (Stmt stmt : stmts) {

            // Look for null check: an if-statement comparing something to null
            // In Jimple: if r1 == null goto ... OR if r1 != null goto ...
            if (!foundNullCheck && stmt instanceof JIfStmt ifStmt) {
                // Java 16 pattern matching for instanceof ^^^
                // This replaces: if (stmt instanceof JIfStmt) { JIfStmt ifStmt = (JIfStmt) stmt; }
                if (isNullComparison(ifStmt)) {
                    foundNullCheck = true;
                }
            }

            // Look for instanceof: an assignment like $z0 = r1 instanceof MyClass
            if (!foundInstanceOf && stmt instanceof JAssignStmt assignStmt) {
                Value rightOp = assignStmt.getRightOp();
                if (rightOp instanceof JInstanceOfExpr instanceOfExpr) {
                    foundInstanceOf = true;
                    instanceOfType = instanceOfExpr.getCheckType().toString();
                }
            }

            // Look for cast: an assignment like r2 = (MyClass) r1
            if (foundInstanceOf && !foundCast && stmt instanceof JAssignStmt assignStmt) {
                Value rightOp = assignStmt.getRightOp();
                if (rightOp instanceof JCastExpr castExpr) {
                    foundCast = true;
                    castType = castExpr.getType().toString();
                }
            }
        }

        // Report findings
        if (!foundNullCheck) {
            results.add(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "equals() method does not check for null argument. " +
                    "The first step should be: if (obj == null) return false; " +
                    "equals() must never throw NullPointerException."
            ));
        }

        if (!foundInstanceOf) {
            results.add(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "equals() method does not use instanceof to check the argument type. " +
                    "After the null check, use: if (!(obj instanceof MyClass)) return false;"
            ));
        }

        if (foundInstanceOf && !foundCast) {
            results.add(new AnalysisResult.Violation(
                    className,
                    GUIDELINE_NAME,
                    severity(),
                    "equals() method has an instanceof check but no subsequent cast. " +
                    "After checking instanceof, cast the argument to the target type."
            ));
        }

        // Verify that instanceof and cast use the same type
        if (foundInstanceOf && foundCast && instanceOfType != null && castType != null) {
            if (!instanceOfType.equals(castType)) {
                results.add(new AnalysisResult.Violation(
                        className,
                        GUIDELINE_NAME,
                        severity(),
                        "equals() method uses instanceof with type '" + instanceOfType +
                        "' but casts to type '" + castType + "'. " +
                        "The instanceof check and cast should use the same type."
                ));
            }
        }

        // If no violations found, it's a pass
        if (results.isEmpty()) {
            results.add(new AnalysisResult.Pass(className, GUIDELINE_NAME));
        }

        return results;
    }

    /**
     * Check if a conditional statement compares something against null.
     * In Jimple, this looks like: if r1 == null or if r1 != null
     */
    private boolean isNullComparison(JIfStmt ifStmt) {
        Value condition = ifStmt.getCondition();
        if (condition instanceof AbstractConditionExpr condExpr) {
            Value op1 = condExpr.getOp1();
            Value op2 = condExpr.getOp2();
            return (op1 instanceof NullConstant || op2 instanceof NullConstant);
        }
        return false;
    }
}
