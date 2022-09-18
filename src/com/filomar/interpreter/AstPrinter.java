package com.filomar.interpreter;

public class AstPrinter implements Expr.Visitor<String> { //DEPRECATED class
    String stringify(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) { //not implemented
        return null;
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return "(" + expr.left.accept(this) + " " + expr.operator.lexeme + " " + expr.right.accept(this) + ")";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "(" + expr.operator.lexeme + " " + expr.expression.accept(this) + ")";
    }

    @Override
    public String visitCallExpr(Expr.Call expr) { //not implemented
        return null;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "grp" + expr.expression.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) { //not implemented
        return null;
    }
}
