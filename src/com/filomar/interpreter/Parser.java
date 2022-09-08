package com.filomar.interpreter;

import java.util.List;

import static com.filomar.interpreter.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    //Top down parsing
    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
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

        while (match(SLASH, STAR)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token op = previous();
            Expr right = unary();
            return new Expr.Unary(op, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NULL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after opening parenthesis.");
            return new Expr.Grouping(expr);
        }

        throw error(current(), "Expected an expression, found '" + current().lexeme + "'.");
    }

    //Error detection and synchronization
    private ParseError error(Token token, String message) {
        Flex.onErrorDetected(token.line, token.column, message);
        return new ParseError();
    }

    private void synchronize() { //check for error safety
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
