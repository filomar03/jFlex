package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.filomar.interpreter.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int next = 0;
    private int line = 1;
    private int column = 0;
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("null", NULL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = next;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line, column + 1));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case ';' -> addToken(SEMICOLON);
            case '-' -> addToken(MINUS);
            case '%' -> addToken(MODULUS);
            case '+' -> addToken(PLUS);
            case '/' -> {
                if (match('/')) lineCommentHandler();
                else if (match('*')) blockCommentHandler();
                else addToken(SLASH);
            }
            case '*' -> {
                if (match('/')) reportError("Closing an non opened comment block.");
                else addToken(STAR);
            }
            case ' ', '\t', '\r', '\n' -> {} //ignore blanks, managed by advance()
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '"' -> stringLiteralHandler();
            default -> {
                if (isDigit(c)) numberLiteralHandler();
                else if (isAlpha(c)) identifierHandler();
                else reportError("Unexpected character '" + c +  "'.");
            }
        }
    }

    //Error reporting
    private void reportError(String message) {
        Flex.onErrorDetected(line, column, message);
    }

    private void reportError(int line, int column, String message) {
        Flex.onErrorDetected(line, column, message);
    }

    //Type checking
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    //Complex tokens handlers
    private void stringLiteralHandler() {
        int stringStartLine = line;
        int stringStartColumn = column;

        while (!isAtEnd() && peek() != '"') advance();

        if (isAtEnd()) reportError(stringStartLine, stringStartColumn, "Unterminated string.");
        else {
            advance();
            addToken(STRING, source.substring(start + 1, next - 1));
        }
    }

    private void numberLiteralHandler() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peek(2))) advance(2);

        while (isDigit(peek())) advance();

        addToken(NUMBER, Double.parseDouble(source.substring(start, next)));
    }

    private void identifierHandler() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, next);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    //Comments handlers
    private void lineCommentHandler() {
        while (!isAtEnd())
            if (advance() == '\n') return;
    }

    private void blockCommentHandler() {
        int blockStartLine = line;
        int blockStartColumn = column - 1;

        while (!isAtEnd())
            if (advance() == '*' && match('/')) return;

        reportError(blockStartLine, blockStartColumn, "Unclosed comment block.");
    }

    //Source management
    private boolean isAtEnd(int steps) { //call to ensure EOF safety
        return next + steps - 1 >= source.length();
    }

    private boolean isAtEnd() {
        return isAtEnd(1);
    }

    private char advance() { //not EOF safe
        char c = source.charAt(next++);
        column++;
        if (c == '\n') {
            line++;
            column = 0;
        }
        return c;
    }

    private char advance(int steps) { //not EOF safe
        if (steps > 1)
            for (int i = 1; i < steps; i++)
                advance();

        return advance();
    }

    private char peek(int steps) { //EOF safe
        if (isAtEnd(steps)) return '\0';
        return source.charAt(next + steps - 1);
    }

    private char peek() {
        return peek(1);
    }

    private boolean match(char expected) { //EOF safe
        return !isAtEnd() && peek() == expected && advance() == expected;
    }

    //Token list management
    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, next);
        tokens.add(new Token(type, lexeme, literal, line, column - lexeme.length() + 1));
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }
}