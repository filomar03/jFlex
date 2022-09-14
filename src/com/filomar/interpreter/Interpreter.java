package com.filomar.interpreter;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Flex.onRuntimeError(error);
        }
    }

    //Statement execution
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitVarDclStmt(Stmt.VarDcl stmt) {
        environment.newBinding(stmt.identifier.lexeme, evaluate(stmt.initializer));
        return null;
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
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruth(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.value)));
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        Environment outerEnv = this.environment;
        this.environment = new Environment(outerEnv);

        interpret(block.statements); //why doesn't the author use this method

        this.environment = outerEnv;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    //Expression evaluation
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.expr);
        environment.setValue(expr.identifier, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        switch (expr.op.type) { //DIY short-circuiting even if java operators already have it
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

        switch (expr.operator.type) {
            case SLASH -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left * (double) right;
            }
            case MODULUS -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left % (double) right;
            }
            case MINUS -> {
                checkNumericOperand(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case GREATER -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left > (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() > ((String) right).length();
                }

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left >= (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() >= ((String) right).length();
                }

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case LESS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left < (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() < ((String) right).length();
                }

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
            }
            case LESS_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left <= (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return ((String) left).length() <= ((String) right).length();
                }

                throw new RuntimeError(expr.operator, "Operator '" + expr.operator.lexeme + "' expected numbers or strings.");
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
        Object right = evaluate(expr.expr);

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
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.getValue(expr.identifier);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expr);
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
