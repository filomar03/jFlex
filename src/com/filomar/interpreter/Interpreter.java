package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Environment globals = new Environment();
    private Environment environment = globals;
    private final HashMap<Expr, Integer> locals = new HashMap<>();

    //Constructors
    Interpreter() {
        globals.create(new Token(IDENTIFIER, "clock", null, 0, 0), new FlexCallable() {
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
    //--Store resolver analysis results
    void resolve(Expr expr, int distance) {
        locals.put(expr, distance);
    }

    //--Interpreter core methods
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
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
        } finally {
            this.environment = previous;
        }
    }

    //--Environment interaction
    private Object lookUpVariable(Expr expr, Token identifier) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(identifier.lexeme(), distance);
        } else {
            return globals.get(identifier);
        }
    }

    //--Visitor pattern type matching (statements)
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    //--Visitor pattern implementations (declarations)
    @Override
    public Void visitFunctionDclStmt(Stmt.FunctionDcl stmt) {
        environment.create(stmt.identifier, new FlexFunction(stmt.identifier.lexeme(), stmt.function, environment));
        return null;
    }

    @Override
    public Void visitVariableDclStmt(Stmt.VariableDcl stmt) {
        environment.create(stmt.identifier, stmt.initializer != null ? evaluate(stmt.initializer) : null);
        return null;
    }

    //--Visitor pattern implementations (statements)
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
        System.out.println(stringify(evaluate(stmt.expression)));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        throw new ReturnEx(stmt.expression != null ? evaluate(stmt.expression) : null);
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

    //--Visitor pattern type matching (expressions)
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    //--Visitor pattern implementations (expressions)
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.expression);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(expr.target.lexeme(), value, distance);
        } else {
            globals.assign(expr.target, value);
        }

        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        switch (expr.operator.type()) { //DIY short-circuiting even if java operators already have it
            case AND -> {
                if (!isTruth(left))
                    return left;
            }
            case OR -> {
                if (isTruth(left))
                    return left;
            }
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case SLASH -> {
                checkNumericOperand(expr.operator, "All operands must be numbers", left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperand(expr.operator, "All operands must be numbers", left, right);
                return (double) left * (double) right;
            }
            case MODULUS -> {
                checkNumericOperand(expr.operator, "All operands must be numbers", left, right);
                return (double) left % (double) right;
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, "All operands must be numbers", left, right);
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

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                if (left instanceof String)
                    left = (double) ((String) left).length();

                if (right instanceof String)
                    right = (double) ((String) right).length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
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
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object expression = evaluate(expr.expression);

        switch (expr.operator.type()) {
            case BANG -> {
                return !isTruth(expression);
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, "All operands must be numbers", expr.expression);
                return -(Double) expression;
            }
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }

        if (!(callee instanceof FlexCallable)) {
            throw new RuntimeError(expr.locationRef, "Callee cannot be called, only function and classes can be called");
        }

        if (args.size() != ((FlexCallable) callee).arity()) {
            throw new RuntimeError(expr.locationRef, "Expected " + ((FlexCallable) callee).arity() + " argument/s, found " + args.size());
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
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(expr.identifier.lexeme(), distance);
        } else {
            return globals.get(expr.identifier);
        }
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
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
