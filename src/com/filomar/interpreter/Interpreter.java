package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.List;

import static com.filomar.interpreter.TokenType.IDENTIFIER;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    //Nested classes
    private static class BreakEx extends RuntimeException {}
    private static class ReturnEx extends RuntimeException {
        final Object value;

        ReturnEx(Object value) {
            this.value = value;
        }
    }

    //Fields
    final Environment globals = new Environment();
    protected Environment environment = globals;

    //Constructors
    Interpreter() {
        globals.createBinding(new Token(IDENTIFIER, "clock", null, 0, 0), new FlexCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis();
            }

            @Override
            public String toString() {
                return "<native fun> clock";
            }
        });
    }

    //Methods
    //--Statements executions
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Flex.onRuntimeError(error);
        }
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Flex.onRuntimeError(error);
        } finally {
            this.environment = previous;
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    //--Visitor pattern declarations interpretation
    @Override
    public Void visitFunctionDclStmt(Stmt.FunctionDcl stmt) {
        environment.createBinding(stmt.identifier, new FlexFunction(stmt.identifier.lexeme(), stmt.function, environment));
        return null;
    }

    @Override
    public Void visitVariableDclStmt(Stmt.VariableDcl stmt) {
        environment.createBinding(stmt.identifier, evaluate(stmt.initializer));
        return null;
    }

    //--Visitor pattern statements interpretation
    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        executeBlock(block.statements, new Environment(this.environment));
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakEx();
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruth(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.thenBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.value)));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = evaluate(stmt.value);
        throw new ReturnEx(value);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruth(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakEx ex) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    //--Visitor pattern expressions evaluation
    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expression) {
        Object value = evaluate(expression.expression);
        environment.setBinding(expression.identifier, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expression) {
        Object left = evaluate(expression.left);

        switch (expression.operator.type()) { //DIY short-circuiting even if java operators already have it
            case AND -> {
                if (!isTruth(left))
                    return left;
            }
            case OR -> {
                if (isTruth(left))
                    return left;
            }
        }

        return evaluate(expression.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expression) {
        Object left = evaluate(expression.left);
        Object right = evaluate(expression.right);

        switch (expression.operator.type()) {
            case SLASH -> {
                checkNumericOperand(expression.operator, "All operands must be numbers", left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperand(expression.operator, "All operands must be numbers", left, right);
                return (double) left * (double) right;
            }
            case MODULUS -> {
                checkNumericOperand(expression.operator, "All operands must be numbers", left, right);
                return (double) left % (double) right;
            }
            case MINUS -> {
                checkNumericOperand(expression.operator, "All operands must be numbers", left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                return stringify(left) + stringify(right);
            }
            case GREATER -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expression.operator, "All operands must be numbers or strings", left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expression.operator, "All operands must be numbers or strings", left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expression.operator, "All operands must be numbers or strings", left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expression.operator, "All operands must be numbers or strings", left, right);
                return (double) left <= (double) right;
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
        }

        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expression) {
        Object right = evaluate(expression.expression);

        switch (expression.operator.type()) {
            case BANG -> {
                return !isTruth(right);
            }
            case MINUS -> {
                checkNumericOperand(expression.operator, "All operands must be numbers", right);
                return -(Double) right;
            }
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expression) {
        Object callee = evaluate(expression.callee);

        List<Object> args = new ArrayList<>();
        for (Expr arg : expression.arguments) {
            args.add(evaluate(arg));
        }

        if (!(callee instanceof FlexCallable)) {
            throw new RuntimeError(expression.paren, "Callee cannot be called, only function and classes can be called");
        }

        if (args.size() != ((FlexCallable) callee).arity()) {
            throw new RuntimeError(expression.paren, "Expected " + ((FlexCallable) callee).arity() + " argument/s, found " + args.size());
        }

        try {
            return ((FlexFunction) callee).call(this, args);
        } catch (ReturnEx returnEx) {
            return returnEx.value;
        }
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new FlexFunction("(anonymous)", expr, environment);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expression) {
        return expression.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expression) {
        return environment.getBinding(expression.identifier);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expression) {
        return evaluate(expression.expression);
    }

    //--Utilities
    private boolean isTruth(Object obj) {
        if (obj instanceof Boolean) return (boolean) obj;
        if (obj instanceof Double) return (double) obj != 0;
        return obj != null;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null) {
            return b == null;
        }

        return a.equals(b);
    }

    private void checkNumericOperand(Token operator, String message, Object ... operands) {
        for (Object operand : operands) {
            if (!(operand instanceof Double)) throw new RuntimeError(operator, message);
        }
    }

    private String stringify(Object a) {
        if (a == null) return "null";
        if (a instanceof Double) {
            if ((double) a % 1 == 0) {
                String str = a.toString();
                return str.substring(0, str.length() - 2);
            }
        }
        return a.toString();
    }
}
