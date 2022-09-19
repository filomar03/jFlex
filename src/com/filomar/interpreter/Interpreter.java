package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    //Nested classes
    private static class BreakEx extends RuntimeException {}
    private static class ReturnEx extends RuntimeException {
        final Object value;

        ReturnEx(Object value) {
            this.value = value;
        }

        Object getValue() {
            return value;
        }
    }

    //Fields
    final Environment globals = new Environment();
    protected Environment environment = globals;

    //Constructors
    Interpreter() {
        globals.createBinding("clock", new FlexCallable() {
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
                return "<native clock fun>";
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
    public Void visitFunDclStmt(Stmt.FunDcl stmt) {
        FlexFunction function = new FlexFunction(stmt);
        environment.createBinding(stmt.identifier.lexeme, function);
        return null;
    }

    @Override
    public Void visitVarDclStmt(Stmt.VarDcl stmt) {
        environment.createBinding(stmt.identifier.lexeme, evaluate(stmt.initializer));
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

        switch (expression.operator.type) { //DIY short-circuiting even if java operators already have it
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

        switch (expression.operator.type) {
            case SLASH -> {
                checkNumericOperand(expression.operator, left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperand(expression.operator, left, right);
                return (double) left * (double) right;
            }
            case MODULUS -> {
                checkNumericOperand(expression.operator, left, right);
                return (double) left % (double) right;
            }
            case MINUS -> {
                checkNumericOperand(expression.operator, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(expression.operator, "Expected operands to be number or string");
            }
            case GREATER -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left > (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() > ((String) right).length();
                }

                throw new RuntimeError(expression.operator, "Expected both operands to be number or string");
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left >= (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() >= ((String) right).length();
                }

                throw new RuntimeError(expression.operator, "Expected both operands to be number or string");
            }
            case LESS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left < (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() < ((String) right).length();
                }

                throw new RuntimeError(expression.operator, "Expected both operands to be number or string");
            }
            case LESS_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left <= (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() <= ((String) right).length();
                }

                throw new RuntimeError(expression.operator, "Expected both operands to be number or string");
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

        switch (expression.operator.type) {
            case BANG -> {
                return !isTruth(right);
            }
            case MINUS -> {
                checkNumericOperand(expression.operator, right);
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

        if (!(callee instanceof FlexCallable function)) {
            throw new RuntimeError(expression.paren, "Callee cannot be called, only function and classes can be called");
        }

        if (args.size() != function.arity()) {
            throw new RuntimeError(expression.paren, "Expected " + function.arity() + " argument/s, found " + args.size());
        }

        try {
            return function.call(this, args);
        } catch (ReturnEx returnEx) {
            return returnEx.value;
        }
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

    private void checkNumericOperand(Token operator, Object ... operands) {
        for (Object operand : operands) {
            if (!(operand instanceof Double)) throw new RuntimeError(operator, "Expected all operands to be number");
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
