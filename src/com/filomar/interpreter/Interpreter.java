package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        globals.create("clock", new FlexCallable() {
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
                return "(native fun)clock";
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

    //--Visitor pattern type matching (statements)
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    //--Visitor pattern implementations (declarations)
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.create(stmt.identifier.lexeme(), null);
        FlexClass klass = new FlexClass(stmt.identifier.lexeme());
        environment.assign(stmt.identifier, klass);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        environment.create(stmt.identifier.lexeme(), null);
        FlexFunction function = new FlexFunction(stmt.identifier.lexeme(), stmt.function, environment);
        environment.assign(stmt.identifier, function);
        return null;
    }

    @Override
    public Void visitVariableStmt(Stmt.Variable stmt) {
        environment.create(stmt.identifier.lexeme(), stmt.initializer != null ? evaluate(stmt.initializer) : null);
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
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.instance);

        if (object instanceof FlexInstance instance) {
            Object value = evaluate(expr.value);
            instance.set(expr.property, value);
            return value;
        }

        throw new RuntimeError(expr.property, "Cannot set propriety of an object that is not an instance (Non dovrebbe succedere, in quanto lerrore dovrebbe essere gia dato da GetExpr)");
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
            default -> {}
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
                if (left instanceof Double a && right instanceof Double b) {
                    return a + b;
                }

                return stringify(left) + stringify(right);
            }
            case GREATER -> {
                if (left instanceof String a)
                    left = a.length();

                if (right instanceof String b)
                    right = b.length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                if (left instanceof String a)
                    left = a.length();

                if (right instanceof String b)
                    right = b.length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                if (left instanceof String a)
                    left = a.length();

                if (right instanceof String b)
                    right = b.length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                if (left instanceof String a)
                    left = a.length();

                if (right instanceof String b)
                    right = b.length();

                checkNumericOperand(expr.operator, "All operands must be numbers or strings", left, right);
                return (double) left <= (double) right;
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + expr.operator.type());
        }
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object expression = evaluate(expr.expression);

        switch (expr.operator.type()) {
            case BANG -> {
                return !isTruth(expression);
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, "Operand must be number", expr.expression);
                return -(double) expression;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + expr.operator.type());
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }

        if (!(callee instanceof FlexCallable callable)) {
            throw new RuntimeError(expr.locationReference, "Callee cannot be called, only function and classes can be called");
        }

        if (args.size() != callable.arity()) {
            throw new RuntimeError(expr.locationReference, "Expected " + callable.arity() + " argument/s, found " + args.size());
        }

        try {
            return callable.call(this, args);
        } catch (ReturnEx ex) {
            return ex.value;
        }
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.instance);

        if (object instanceof FlexInstance instance) {
            return instance.get(expr.property);
        }
        
        throw new RuntimeError(expr.property, "Trying to retrieve a property from an object that is not an instance");
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new FlexFunction("(anonym fun)", expr, environment);
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
