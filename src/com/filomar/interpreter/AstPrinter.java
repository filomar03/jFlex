/*
    This class stringify AST nodes
        Override toString() methods of Expr.class and Stmt.class with these to gather useful string when using the debugger.
        Example:
            @Override
            public String toString() {
                return Flex.debugAstPrinter().stringify(this);
            }

    Remove stringify, since when concatenating strings java implicitly calls toString methods, that's already been overrided in Expr e Stmt classes
*/

/* package com.filomar.interpreter;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    //Methods
    //--Visitor pattern type matching
    String stringify(Expr expr) {
        return expr.accept(this);
    }

    String stringify(Stmt stmt) {
        return stmt.accept(this);
    }

    //--Visitor pattern implementations
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        
        return expr.target.lexeme() + " = " + expr.expression.accept(this);
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        
        return expr.instance.accept(this) + " = " + expr.property.lexeme();
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
            if (!arg.equals(expr.arguments.get(expr.arguments.size() - 1)))
                builder.append(", ");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        
        return stringify(expr.instance) + "." + expr.property;
    }

    @Override
    public String visitFunctionExpr(Expr.Function expr) {
        
        StringBuilder builder = new StringBuilder();
        builder.append("fun <lambda/anonymous>");
        builder.append("(");
        for (Token param : expr.parameters) {
            builder.append(param.lexeme());
            if (!param.equals(expr.parameters.get(expr.parameters.size() - 1)))
                builder.append(", ");
        }
        builder.append(")");
        if (!concise) {
            builder.append(" {\n");
            for (Stmt stmt : expr.body) {
                builder.append("\t");
                builder.append(stmt.accept(this));
                builder.append("\n");
            }
            builder.append("}");
        }
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
        return '"' + expr.value.toString() + '"';
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.identifier.lexeme();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.accept(this) + ")";
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("class ");
        builder.append(stmt.identifier.lexeme());
        if (concise) return builder.toString();
        builder.append("{\n");
        for (Stmt.Function method : stmt.methods) {
            builder.append("\tfun ");
            builder.append(method.identifier.lexeme());
            builder.append("(");
            for (Token param : method.function.parameters) {
                builder.append(param);
                if (param != method.function.parameters.get(method.function.parameters.size() - 1))
                    builder.append(", ");
            }
            builder.append(")\n");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("fun ");
        builder.append(stmt.identifier.lexeme());
        builder.append("(");
        for (Token param : stmt.function.parameters) {
            builder.append(param.lexeme());
            if (!param.equals(stmt.function.parameters.get(stmt.function.parameters.size() - 1)))
                builder.append(", ");
        }
        builder.append(")");
        if (!concise) {
            builder.append(" ");
            builder.append(" {\n");
            for (Stmt bodyStmt : stmt.function.body) {
                builder.append("\t");
                builder.append(bodyStmt.accept(this));
                builder.append("\n");
            }
            builder.append("}");
        }
        return builder.toString();
    }

    @Override
    public String visitVariableStmt(Stmt.Variable stmt) {
        return "var " + stmt.identifier.lexeme() + " = " + stmt.initializer.accept(this) + ";";
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        if (concise) {
            return "block statement (" + stmt.statements.size() + ")";
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            for (Stmt statement : stmt.statements) {
                builder.append("\t");
                builder.append(statement.accept(this));
                builder.append("\n");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return "break;";
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("if (");
        builder.append(stmt.condition.accept(this));
        builder.append(")");
        if (!concise) {
            builder.append(" ");
            builder.append(stmt.thenBranch.accept(this));
            if (stmt.elseBranch != null) {
                builder.append(" else ");
                builder.append(stmt.elseBranch.accept(this));
            }
        }
        return builder.toString();
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return "print " + stmt.expression.accept(this) + ";";
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.expression != null) {
            return "return " + stmt.expression.accept(this) + ";";
        } else {
            return "return;";
        }
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        if (concise) {
            return "while (" + stmt.condition.accept(this) + ")";
        } else {
            return "while (" + stmt.condition.accept(this) + ")" + stmt.body.accept(this);
        }
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return stmt.expression.accept(this) + ";";
    }
}
 */