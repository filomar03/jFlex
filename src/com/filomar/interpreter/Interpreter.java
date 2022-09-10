package com.filomar.interpreter;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    void interpret(List<Stmt> statements) {
        for (Stmt statement : statements) {
            try {
                execute(statement);
            } catch (RuntimeError error) {
                Flex.onRuntimeError(error);
            }
        }
    }

    //Statement execution
    void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.value)));
        return null;
    }

    //Expression evaluation
    Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case GREATER -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left > (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() > ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left >= (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() >= ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case LESS -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left < (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() < ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case LESS_EQUAL -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left <= (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() <= ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case MODULUS -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left % (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;

                if (left instanceof String || right instanceof String)
                    return stringify(left) + stringify(right);

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case SLASH -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left * (double) right;
            }
        }

        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG -> {
                return !isTruth(right);
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, right);
                return -(Double) right;
            }
        }

        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expr);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    //Utility methods
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
            if (!(operand instanceof Double)) throw new RuntimeError(operator, "Operator '" + operator.lexeme + "' expected numbers.");
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
