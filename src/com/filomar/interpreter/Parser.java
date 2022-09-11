package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.List;

import static com.filomar.interpreter.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }

        return statements;
    }

    //statement parsing
    private Stmt statement() {
        try {
            if (match(VAR)) return declarationStmt();
            if (match(PRINT)) return printStmt();
            return expressionStmt();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt declarationStmt() {
        Token identifier = consume(IDENTIFIER, "Expected variable name after definition, found '" + current().lexeme + "' instead.");

        Expr initializer = new Expr.Literal(null);
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' at the end of the statement, found '" + current().lexeme + "' instead.");
        return new Stmt.VarDcl(identifier, initializer);
    }

    private Stmt expressionStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' at the end of the statement, found '" + current().lexeme + "' instead.");
        return new Stmt.Expression(expr);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' at the end of the statement found '" + current().lexeme + "' instead.");
        return new Stmt.Print(value);
    }

    //Top down expression parsing
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token op = previous();
            Expr right = assignment();

            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).identifier, right);
            }

            throw error(op, "Invalid assignment target");
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token op = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR, MODULUS)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token op = previous();
            Expr expr = unary();
            return new Expr.Unary(op, expr);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NULL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')', found '" + current().lexeme + "' instead.");
            return new Expr.Grouping(expr);
        }

        throw error(current(), "Expected an expression, found '" + current().lexeme + "' instead.");
    }

    //Error reporting and recovery
    private ParseError error(Token token, String message) {
        Flex.onErrorDetected(token.line, token.column, message);
        return new ParseError();
    }

    private void synchronize() { //come back and check this function, when it will be used in code
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (current().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> { return; }
            }
        }

        advance();
    }

    //Token list management
    private Token consume(TokenType type, String message) throws ParseError { //EOF safe
        if (match(type)) return previous();

        throw error(current(), message);
    }

    private boolean match(TokenType... types) { //EOF safe
        if (isAtEnd()) return false;
        for (TokenType type : types) {
            if (current().type == type) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() { //EOF safe
        if (!isAtEnd()) current++;
        return current();
    }

    private boolean isAtEnd() { //EOF safe
        return current().type == EOF;
    }

    private Token previous() { //not EOF safe
        return tokens.get(current - 1);
    }

    private Token current() { //EOF safe
        return tokens.get(current);
    }
}
