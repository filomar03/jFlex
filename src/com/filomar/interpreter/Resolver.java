package com.filomar.interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    //Enums
    private enum FunctionType {
        NONE,
        FUNCTION
    }

    //Fields
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunciton = FunctionType.NONE;

    //Constructors
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    //Methods
    //--Core method
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    //--Manage scopes stack
    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void exitScope() {
        scopes.pop();
    }

    //--Manage bindings map
    private void declare(Token identifier) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(identifier.lexeme())) {
            Flex.onErrorDetected(identifier, "A variable with this name already exists in teh current scope");
        }

        scope.put(identifier.lexeme(), false);
    }

    private void define(Token identifier) {
        if (scopes.isEmpty()) return;

        scopes.peek().put(identifier.lexeme(), true);
    }

    //--Visitor pattern type matching
    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expression) {
        expression.accept(this);
    }

    //--Visitor pattern implementations
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.expression);
        resolveLocal(expr, expr.target);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.arguments) {
            resolve(arg);
        }
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {
        resolveFunction(expr, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.identifier.lexeme()) == Boolean.FALSE) {
            Flex.onErrorDetected(expr.identifier, "Variable cannot refer to itself in it's own initializer");
        }

        resolveLocal(expr, expr.identifier);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitFunctionDclStmt(Stmt.FunctionDcl stmt) {
        declare(stmt.identifier);
        define(stmt.identifier);
        resolveFunction(stmt.function, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitVariableDclStmt(Stmt.VariableDcl stmt) {
        declare(stmt.identifier);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.identifier);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        exitScope();
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunciton == FunctionType.NONE) {
            Flex.onErrorDetected(stmt.keyword, "Cannot return at top level code");
        }

        if (stmt.expression != null) resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    //--Resolve local bindings
    private void resolveLocal(Expr expr, Token identifier) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(identifier.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Expr.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunciton;
        currentFunciton = type;

        beginScope();
        for (Token param : function.parameters) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        exitScope();

        currentFunciton = enclosingFunction;
    }
}
