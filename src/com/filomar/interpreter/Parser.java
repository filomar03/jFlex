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
            statements.add(declaration());
        }

        return statements;
    }

    //statement parsing
    private Stmt declaration() {
        if (match(VAR)) return varDclStmt();
        return statement();
    }

    private Stmt varDclStmt() {
        Token identifier = consume(IDENTIFIER, "Expected variable name after definition, found '" + current().lexeme + "' instead.");

        Expr initializer = new Expr.Literal(null);
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' at the end of the statement, found '" + current().lexeme + "' instead.");
        return new Stmt.VarDcl(identifier, initializer);
    }

    private Stmt statement() {
        try {
            if (match(IF)) return branchingStmt();
            if (match(PRINT)) return printStmt();
            if (match(LEFT_BRACE)) return new Stmt.Block(stmtCollector());
            return expressionStmt();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt branchingStmt() {
        consume(LEFT_PAREN, "Expected '('.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')'.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.Branching(condition, thenBranch, elseBranch);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' at the end of the statement found '" + current().lexeme + "' instead.");
        return new Stmt.Print(value);
    }

    private List<Stmt> stmtCollector() {
        List<Stmt> statements = new ArrayList<>();

        while (current().type != RIGHT_BRACE && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' at the end of the block statement, found '" + current().lexeme + "' instead.");
        return statements;
    }

    private Stmt expressionStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' at the end of the statement, found '" + current().lexeme + "' instead.");
        return new Stmt.Expression(expr);
    }

    //Top down expression parsing
    private Expr expression() {
        return assignmentExpr();
    }

    private Expr assignmentExpr() {
        Expr expr = logicalOrExpr();

        if (match(EQUAL)) {
            Token op = previous();
            Expr right = assignmentExpr();

            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).identifier, right);
            }

            throw error(op, "Invalid assignment target");
        }

        return expr;
    }

    private Expr logicalOrExpr() { //separated in two methods to assure AND op has precedence over OR op
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

        return primaryExpr();
    }

    private Expr primaryExpr() {
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

    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (current().type) {
                case CLASS, FUN, FOR, IF, PRINT, RETURN, VAR, WHILE -> { return; }
            }

            advance();
        }
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
