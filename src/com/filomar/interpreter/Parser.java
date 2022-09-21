package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.filomar.interpreter.TokenType.*;

public class Parser {
    //Nested classes
    private static class ParseError extends RuntimeException {}

    //Fields
    private final List<Token> tokens;
    private int current = 0;
    private int loopDepth = 0;

    //Constructors
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    //Methods
    //--Parsing
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    //--Declarations parsing
    private Stmt declaration() {
        if (match(FUN)) return funDclStmt();
        if (match(VAR)) return varDclStmt();
        return statement();
    }

    private Stmt funDclStmt() {
        Token identifier = consume(IDENTIFIER, "Expected a valid function/method name");
        consume(LEFT_PAREN, "Expected '(' after function/method name");
        List<Token> parameters = new ArrayList<>();
        if (!match(RIGHT_PAREN)) {
            do {
                if (parameters.size() < 255) {
                    parameters.add(consume(IDENTIFIER, "Expected a valid parameter name"));
                } else {
                    throw error(current(), "Function/Method definitions cannot have more than 255 parameters");
                }
            } while (match(COMMA));
            consume(RIGHT_PAREN, "Expected ')' after parameters");
        }
        consume(LEFT_BRACE, "Expect '{' before function/method body");
        List<Stmt> body = blockCollector();

        return new Stmt.FunDcl(identifier, parameters, body);
    }

    private Stmt varDclStmt() {
        Token identifier = consume(IDENTIFIER, "Expected a valid variable name");

        Expr initializer = new Expr.Literal(null);
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' at the end of a statement");
        return new Stmt.VarDcl(identifier, initializer);
    }

    //--Statements parsing
    private Stmt statement() {
        try {
            if (match(LEFT_BRACE)) return new Stmt.Block(blockCollector());
            if (match(BREAK)) return breakStmt();
            if (match(FOR)) return forStmt();
            if (match(IF)) return ifStmt();
            if (match(PRINT)) return printStmt();
            if (match(RETURN)) return returnStmt();
            if (match(WHILE)) return whileStmt();
            return expressionStmt();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private List<Stmt> blockCollector() {
        List<Stmt> statements = new ArrayList<>();

        while (current().type() != RIGHT_BRACE && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' at the end of a block statement");
        return statements;
    }

    private Stmt breakStmt() {
        if (loopDepth == 0) {
            throw error(current(), "Cannot use 'break' outside a loop.");
        }

        consume(SEMICOLON, "Expected ';' at the end of a statement");
        return new Stmt.Break();
    }

    private Stmt forStmt() { //Syntactic sugar, parsed as a 'WHILE' --> see scripts/for_loop_issue.flx
        consume(LEFT_PAREN, "Expected '(' before condition");

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
            consume(SEMICOLON, "Expected ';' after condition");
        }

        Expr increment = null;
        if (!match(RIGHT_PAREN)) {
            increment = expression();
            consume(RIGHT_PAREN, "Expected ')' after update");
        }

        try {
            loopDepth++;
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
        } finally {
            loopDepth--;
        }
    }

    private Stmt ifStmt() {
        consume(LEFT_PAREN, "Expected '(' before condition");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after condition");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' at the end of a statement");
        return new Stmt.Print(value);
    }

    private Stmt returnStmt() {
        Expr value = new Expr.Literal(null);
        if (!match(SEMICOLON)) {
            value = expression();
            consume(SEMICOLON, "Expected ';' at the end of a statement");
        }

        return new Stmt.Return(value);
    }

    private Stmt whileStmt() {
        consume(LEFT_PAREN, "Expected '(' before condition");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after condition");

        try {
            loopDepth++;
            Stmt body = statement();

            return new Stmt.While(condition, body);
        } finally {
            loopDepth--;
        }
    }

    private Stmt expressionStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' at the end of a statement");
        return new Stmt.Expression(expr);
    }

    //--Expression parsing (Top-down)
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

        return callExpr();
    }

    private Expr callExpr() {
        Expr expr = primaryExpr();

        while (true) {
            if (match(LEFT_PAREN)) {
                List<Expr> arguments = new ArrayList<>();
                Token paren;
                if (!match(RIGHT_PAREN)) {
                    do {
                        if (arguments.size() < 255) {
                            arguments.add(expression());
                        } else {
                            throw error(current(), "Calls cannot have more than 255 arguments");
                        }
                    } while (match(COMMA));

                    paren = consume(RIGHT_PAREN, "Expected ')' after arguments");
                } else {
                    paren = previous();
                }

                expr = new Expr.Call(expr, paren, arguments);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primaryExpr() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NULL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal());
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after the expression");
            return new Expr.Grouping(expr);
        }

        throw error(current(), "Expected a primary expression");
    }

    //--Error reporting and recovery
    private ParseError error(Token token, String message) {
        Flex.onErrorDetected(token.line(), token.column(), message);
        return new ParseError();
    }

    private void synchronize() { //not so useful right now, consider a revision in the future
        if (!isAtEnd())
            advance();

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) return;

            switch (current().type()) {
                case CLASS, FUN, FOR, IF, PRINT, RETURN, VAR, WHILE -> { return; }
            }

            advance();
        }
    }

    //--Token list manipulation
    private Token consume(TokenType type, String message) throws ParseError { //EOF safe
        if (match(type)) return previous();

        throw error(current(), message);
    }

    private boolean match(TokenType... types) { //EOF safe
        if (isAtEnd()) return false;
        for (TokenType type : types) {
            if (current().type() == type) {
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
        return current().type() == EOF;
    }

    private Token previous() { //not EOF safe
        return tokens.get(current - 1);
    }

    private Token current() { //EOF safe
        return tokens.get(current);
    }
}
