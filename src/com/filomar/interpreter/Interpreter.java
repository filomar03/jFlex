package com.filomar.interpreter;

public class Interpreter implements Expr.Visitor<Object> {
    private Object evaluate(Expr expr) {
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
            case EQUAL -> {
                return isEqual(left, right);
            }
            case GREATER -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left > (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() > ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected operands of type 'Double' or 'String'");
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left >= (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() >= ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected operands of type 'Double' or 'String'");
            }
            case LESS -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left < (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() < ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected operands of type 'Double' or 'String'");
            }
            case LESS_EQUAL -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left <= (double) right;

                if (left instanceof String && right instanceof String)
                    return ((String) left).length() <= ((String) right).length();

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected operands of type 'Double' or 'String'");
            }
            case MINUS -> {
                checkType(expr.operator, Double.class, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;

                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right; //check if cast is really redundant

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected operands of type 'Double' or 'String'");
            }
            case SLASH -> {
                checkType(expr.operator, Double.class, left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkType(expr.operator, Double.class, left, right);
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
                checkType(expr.operator, Double.class, right);
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

    private <T> void checkType(Token operator, Class<T> type, Object ... operands) {
        for (Object operand : operands) {
            if (type.isInstance(operand)) return;
        }

        throw new RuntimeError(operator, "Operator '" + operator.lexeme + "' expected operand/s of type '" + type.getName() + "'");
    }
}
