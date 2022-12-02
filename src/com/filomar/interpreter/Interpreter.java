package com.filomar.interpreter;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private static class Break extends RuntimeException {}

    private final Environment globals = new Environment();
    private Environment environment = globals;
    private final HashMap<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.create("clock", new FlexCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000;
            }

            @Override
            public String toString() {
                return "[native fun clock]";
            }
        });
    }

    // Store resolver analysis results
    void resolve(Expr expr, int distance) {
        locals.put(expr, distance);
    }

    // Interpret statements
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

    // Visitor pattern type matching (declarations e statements)
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    // Visitor pattern implementations (declarations)
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superClass = null;
        if (stmt.superClass != null) {
            superClass = evaluate(stmt.superClass);
            if (!(superClass instanceof FlexClass)) {
                throw new RuntimeError(stmt.superClass.identifier, "Superclass must be a class");
            }

            environment = new Environment(environment);
            environment.create("super", superClass);
        }

        Map<String, FlexFunction> methods = new HashMap<>();
        for (Stmt.Function function : stmt.methods) {
            FlexFunction method = new FlexFunction(
                    function.identifier.lexeme(),
                    function.function,
                    environment,
                    function.identifier.lexeme().equals("init")
            );
            methods.put(function.identifier.lexeme(), method);
        }

        if (superClass != null) {
            environment = environment.closure;
        }

        environment.create(stmt.identifier.lexeme(), new FlexClass(stmt.identifier.lexeme(), (FlexClass) superClass, methods));

        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        FlexFunction function = new FlexFunction(stmt.identifier.lexeme(), stmt.function, environment, false);
        environment.create(stmt.identifier.lexeme(), function);
        return null;
    }

    @Override
    public Void visitVariableStmt(Stmt.Variable stmt) {
        environment.create(stmt.identifier.lexeme(), stmt.initializer != null ? evaluate(stmt.initializer) : null);
        return null;
    }

    // Visitor pattern implementations (statements)
    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        executeBlock(block.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruth(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.expression)));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        throw new Return(stmt.expression != null ? evaluate(stmt.expression) : null);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruth(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (Break breakEx) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    // Visitor pattern type matching (expressions)
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Visitor pattern implementations (expressions)
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
        Object object = evaluate(expr.object);

        if (object instanceof FlexInstance instance) {
            Object value = evaluate(expr.value);
            instance.set(expr.field, value);
            return value;
        }

        throw new RuntimeError(expr.field, "Cannot set field, object is not an instance");
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        switch (expr.operator.type()) {
            case AND -> {
                if (!isTruth(left))
                    return left;
            }
            case OR -> {
                if (isTruth(left))
                    return left;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + expr.operator.type());
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case SLASH -> {
                checkNumericOperands(expr.operator, "All operands must be numbers", left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperands(expr.operator, "All operands must be numbers", left, right);
                return (double) left * (double) right;
            }
            case MODULUS -> {
                checkNumericOperands(expr.operator, "All operands must be numbers", left, right);
                return (double) left % (double) right;
            }
            case MINUS -> {
                checkNumericOperands(expr.operator, "All operands must be numbers", left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double a && right instanceof Double b) {
                    return a + b;
                }

                return stringify(left) + stringify(right);
            }
            case GREATER -> {
                checkNumericOperands(expr.operator, "Operands must be numbers or strings", left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumericOperands(expr.operator, "Operands must be numbers or strings", left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumericOperands(expr.operator, "Operands must be numbers or strings", left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumericOperands(expr.operator, "Operands must be numbers or strings", left, right);
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
                checkNumericOperand(expr.operator, "Operand must be number", expression);
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
            throw new RuntimeError(expr.paren, "Callee cannot be called, only function and classes can be called");
        }

        if (args.size() != callable.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + callable.arity() + " argument/s, found " + args.size());
        }

        return callable.call(this, args);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);

        if (object instanceof FlexInstance instance) {
            return instance.get(expr.property);
        }

        throw new RuntimeError(expr.property, "Cannot get property, object is not an instance");
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new FlexFunction("[anon fun]", expr, environment, false);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitSelfExpr(Expr.Self expr) {
        //assert locals.get(expr) != null; // Debug purpose only
        return environment.getAt("self", locals.get(expr));
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        //assert locals.get(expr) != null; // Debug purpose only
        int distance = locals.get(expr);
        FlexClass superClass = (FlexClass) environment.getAt("super", distance);
        FlexFunction method = superClass.findMethod(expr.method.lexeme());
        if (method != null) {
            return method.bind((FlexInstance) environment.getAt("self", distance - 1));
        }
        throw new RuntimeError(expr.method, "Method '" + expr.method.lexeme() + "' cannot be found in superclass");
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

    // Utilities
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

    private void checkNumericOperand(Token operator, String message, Object a) {
        if (!(a instanceof Double)) throw new RuntimeError(operator, message);
    }

    private void checkNumericOperands(Token operator, String message, Object a, Object b) {
        if (!(a instanceof Double && b instanceof Double)) throw new RuntimeError(operator, message);
    }

    private String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Double n) {
            if (n % 1 == 0) {
                String str = n.toString();
                return str.substring(0, str.length() - 2);
            }
        }
        return obj.toString();
    }
}
