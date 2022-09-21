package com.filomar.interpreter;

public class AstPrinter implements Expr.Visitor<String> {
    private final Interpreter interpreter;
    AstPrinter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    String stringify(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.identifier.lexeme() + " = " + expr.expression.accept(this);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return expr.left.accept(this) + " " + expr.operator.lexeme() + " " + expr.right.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return expr.left.accept(this) + " " + expr.operator.lexeme() + " " + expr.right.accept(this);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.operator.lexeme() + expr.expression.accept(this);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.callee.accept(this));
        builder.append("(");
        for (Expr arg : expr.arguments) {
            builder.append(arg.accept(this));
            if (arg != expr.arguments.get(expr.arguments.size() - 1))
                builder.append(", ");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "null";
        if (expr.value instanceof Double) {
            if ((double) expr.value % 1 == 0) {
                String str = expr.value.toString();
                return str.substring(0, str.length() - 2);
            }
        }
        return expr.value.toString();
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.identifier.lexeme());
        try {
            Object callee = interpreter.environment.getBinding(expr.identifier);
            if (!(callee instanceof FlexCallable)) {
                builder.append("(");
                try {
                    builder.append(interpreter.environment.getBinding(expr.identifier));
                } catch (RuntimeError error) {
                    builder.append("?");
                }
                builder.append(")");
            }
        } catch (RuntimeError error) {}
        return builder.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.accept(this) + ")";
    }
}
