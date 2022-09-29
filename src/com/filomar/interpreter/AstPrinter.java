/*
    This class stringify AST nodes
        Override toString() methods of Expr.class with these to gather useful string when using the debugger.
        Example:
            @Override
            public String toString() {
                return Flex.debugAstPrinter().stringify(this);
            }
*/

package com.filomar.interpreter;

public class AstPrinter implements Expr.Visitor<String> {
    //Fields
    private final Interpreter interpreter;

    //Constructors
    AstPrinter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    //Methods
    //--Stringify AST node
    String stringify(Expr expr) {
        return expr.accept(this);
    }

    //--Visitor pattern
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
    public String visitFunctionExpr(Expr.Function expr) {
        return "Function expression";
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
        try {
            Object binding = interpreter.environment.getBinding(expr.identifier);
            if (binding instanceof FlexCallable function) {
                builder.append(function);
            } else {
                builder.append(expr.identifier.lexeme());
                builder.append("(");
                builder.append(binding);
                builder.append(")");

            }
        } catch (RuntimeError ignored) {}
        return builder.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.accept(this) + ")";
    }
}
