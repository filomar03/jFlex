package com.filomar.interpreter;

public class AstPrinter implements Expr.Visitor<String> {
    String stringify(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return "(" + expr.left.accept(this) + " " + expr.operator.lexeme + " " + expr.right.accept(this) + ")";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "(" + expr.operator.lexeme + " " + expr.expr.accept(this) + ")";
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "grp" + expr.expr.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }
}
