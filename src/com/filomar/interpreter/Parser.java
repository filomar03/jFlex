package com.filomar.interpreter;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static com.filomar.interpreter.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Start parsing
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // Parse declaration
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classStmt();
            if (check(FUN) && checkNext(IDENTIFIER)) {
                advance();
                return functionStmt();
            }
            if (match(VAR)) return varDclStmt();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classStmt() {
        Token identifier = consume(IDENTIFIER, "Expected a valid class name");
        consume(LEFT_BRACE, "Expected '('");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            Token name = consume(IDENTIFIER, "Expected a valid method name");
            methods.add(new Stmt.Function(name, functionDefinition()));
        }

        consume(RIGHT_BRACE, "Expected '}'");
        return new Stmt.Class(identifier, methods);
    }

    private Stmt functionStmt() {
        Token identifier = advance();
        return new Stmt.Function(identifier, functionDefinition());
    }

    private Stmt varDclStmt() {
        Token identifier = consume(IDENTIFIER, "Expected a valid variable name");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';'");
        return new Stmt.Variable(identifier, initializer);
    }

    // Statements parsing
    private Stmt statement() {
        if (match(LEFT_BRACE)) return new Stmt.Block(blockCollector());
        if (match(BREAK)) return breakStmt();
        if (match(FOR)) return forStmt();
        if (match(IF)) return ifStmt();
        if (match(PRINT)) return printStmt();
        if (match(RETURN)) return returnStmt();
        if (match(WHILE)) return whileStmt();
        return expressionStmt();
    }

    private Stmt breakStmt() {
        consume(SEMICOLON, "Expected ';'");
        return new Stmt.Break(previous());
    }

    private Stmt forStmt() {
        consume(LEFT_PAREN, "Expected '('");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDclStmt();
        } else {
            initializer = expressionStmt();
        }

        Expr condition = null;
        if (!match(SEMICOLON)) {
            condition = expression();
            consume(SEMICOLON, "Expected ';'");
        }

        Expr increment = null;
        if (!match(RIGHT_PAREN)) {
            increment = expression();
            consume(RIGHT_PAREN, "Expected ')'");
        }

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
            ));
        }
        body = new Stmt.While((condition != null ? condition : new Expr.Literal(true)), body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(
                    initializer,
                    body
            ));
        }

        return body;
    }

    private Stmt ifStmt() {
        consume(LEFT_PAREN, "Expected '('");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')'");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';'");
        return new Stmt.Print(value);
    }

    private Stmt returnStmt() {
        Expr value = null;
        if (!match(SEMICOLON)) {
            value = expression();
            consume(SEMICOLON, "Expected ';'");
        }
        return new Stmt.Return(previous(), value);
    }

    private Stmt whileStmt() {
        consume(LEFT_PAREN, "Expected '('");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')'");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';'");
        return new Stmt.Expression(expr);
    }

    // Helper method for parsing block statements
    private List<Stmt> blockCollector() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expected '}'");
        return statements;
    }

    // Helper method for parsing functions
    private Expr.Function functionDefinition() {
        consume(LEFT_PAREN, "Expected '('");
        List<Token> parameters = new ArrayList<>();
        if (!match(RIGHT_PAREN)) {
            do {
                if (parameters.size() < 255) {
                    parameters.add(consume(IDENTIFIER, "Expected a valid parameter"));
                } else {
                    throw error(current(), "Function/Method definitions cannot have more than 255 parameters");
                }
            } while (match(COMMA));
            consume(RIGHT_PAREN, "Expected ')'");
        }

        consume(LEFT_BRACE, "Expect opening brace");
        List<Stmt> body = blockCollector();

        return new Expr.Function(parameters, body);
    }

    // Expression parsing (top-down)
    private Expr expression() {
        return assignmentExpr();
    }

    private Expr assignmentExpr() {
        Expr expr = logicalOrExpr();

        if (match(EQUAL)) {
            Token op = previous();
            Expr right = assignmentExpr();
            if (expr instanceof Expr.Variable var) {
                return new Expr.Assign(var.identifier, right);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.property, right);
            }

            throw error(op, "Invalid assignment target, only variables and fields can be assigned");
        }

        return expr;
    }

    private Expr logicalOrExpr() {
        Expr expr = logicalAndExpr();

        if (match(OR)) {
            Token op = previous();
            Expr right = logicalAndExpr();
            expr = new Expr.Logical(expr, op, right);
        }

        return expr;
    }

    private Expr logicalAndExpr() {
        Expr expr = equalityExpr();

        if (match(AND)) {
            Token op = previous();
            Expr right = comparisonExpr();
            expr = new Expr.Logical(expr, op, right);
        }

        return expr;
    }

    private Expr equalityExpr() {
        Expr expr = comparisonExpr();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token op = previous();
            Expr right = comparisonExpr();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr comparisonExpr() {
        Expr expr = termExpr();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = previous();
            Expr right = termExpr();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr termExpr() {
        Expr expr = factorExpr();

        while (match(MINUS, PLUS)) {
            Token op = previous();
            Expr right = factorExpr();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr factorExpr() {
        Expr expr = unaryExpr();

        while (match(SLASH, STAR, MODULUS)) {
            Token op = previous();
            Expr right = unaryExpr();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr unaryExpr() {
        if (match(BANG, MINUS)) {
            Token op = previous();
            Expr expr = unaryExpr();
            return new Expr.Unary(op, expr);
        }

        return secondaryExpr();
    }

    private Expr secondaryExpr() {
        Expr expr = primaryExpr();

        while (true) {
            if (match(LEFT_PAREN)) {
                Token ref = previous();
                List<Expr> arguments = new ArrayList<>();
                if (!match(RIGHT_PAREN)) {
                    do {
                        if (arguments.size() < 255) {
                            arguments.add(expression());
                        } else {
                            throw error(current(), "Calls cannot have more than 255 arguments");
                        }
                    } while (match(COMMA));
                    consume(RIGHT_PAREN, "Expected ')'");
                }
                expr = new Expr.Call(expr, ref, arguments);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expected property name or method call");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primaryExpr() {
        if (match(FUN)) return functionDefinition();
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')'");
            return new Expr.Grouping(expr);
        }
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NULL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal());
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(SELF)) return new Expr.Self(previous());
        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        throw error(current(), "Expected a primary expression (lambda function, grouping, literal, identifier)");
    }

    // Error reporting and recovery
    private ParseError error(Token token, String message) {
        Flex.onErrorDetected(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) return;

            switch (current().type()) {
                case CLASS, FUN, FOR, IF, PRINT, RETURN, VAR, WHILE -> { return; }
                default -> advance();
            }
        }
    }

    // Token list manipulation
    private Token consume(TokenType type, String message) { // EOF safe
        if (match(type)) return previous();

        throw error(current(), message);
    }

    private boolean match(TokenType... types) { // EOF safe
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean checkNext(TokenType type) { // EOF safe
        return next().type() == type;
    }

    private boolean check(TokenType type) { // EOF safe
        return current().type() == type;
    }

    private Token advance() { // EOF safe
        if (!isAtEnd()) return tokens.get(current++);
        return current();
    }

    private boolean isAtEnd() { // EOF safe
        return current().type() == EOF;
    }

    private Token next() { // EOF safe
        if (isAtEnd()) return current();
        return tokens.get(current + 1);
    }

    private Token previous() { // EOF safe
        if (current == 0) return current();
        return tokens.get(current - 1);
    }

    private Token current() { // EOF safe
        return tokens.get(current);
    }
}
