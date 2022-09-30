package com.filomar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.filomar.interpreter.TokenType.*;

public class Scanner {
    //Fields
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int next = 0;
    private int line = 1;
    private int column = 0;
    private static final Map<String, TokenType> keywords;

    //Static blocks
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("break", BREAK);
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

    //Constructors
    Scanner(String source) {
        this.source = source;
    }

    //Methods
    //--Scanning
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = next;
            scanToken();
        }

        tokens.add(new Token(EOF, "EOF", null, line, column + 1));
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
                if (match('/')) lineCommentHelper();
                else if (match('*')) blockCommentHelper();
                else addToken(SLASH);
            }
            case '*' -> {
                if (match('/')) reportError("Closing an unopened comment block");
                else addToken(STAR);
            }
            case ' ', '\t', '\r', '\n' -> {} //ignore blanks, managed by advance()
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '"' -> stringLiteralHelper();
            default -> {
                if (isDigit(c)) numberLiteralHelper();
                else if (isAlpha(c)) identifierHelper();
                else reportError("Unexpected character '" + c +  "'");
            }
        }
    }

    //--Error reporting
    private void reportError(String message) {
        Flex.onErrorDetected(line, column, message);
    }

    private void reportError(int line, int column, String message) {
        Flex.onErrorDetected(line, column, message);
    }

    //--Type checking
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

    //--Complex token helpers
    private void stringLiteralHelper() {
        int stringStartLine = line;
        int stringStartColumn = column;

        while (!isAtEnd() && peek(1) != '"') advance();

        if (isAtEnd()) reportError(stringStartLine, stringStartColumn, "Unterminated string");
        else {
            advance();
            addToken(STRING, source.substring(start + 1, next - 1));
        }
    }

    private void numberLiteralHelper() {
        while (isDigit(peek(1))) advance();

        if (peek(1) == '.' && isDigit(peek(2))) advance();

        while (isDigit(peek(1))) advance();

        addToken(NUMBER, Double.parseDouble(source.substring(start, next)));
    }

    private void identifierHelper() {
        while (isAlphaNumeric(peek(1))) advance();

        String text = source.substring(start, next);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    //--Comment helpers
    private void lineCommentHelper() {
        while (!isAtEnd())
            if (advance() == '\n') return;
    }

    private void blockCommentHelper() {
        int blockStartLine = line;
        int blockStartColumn = column - 1;

        while (!isAtEnd())
            if (advance() == '*' && match('/')) return;

        reportError(blockStartLine, blockStartColumn, "Unclosed comment block");
    }

    //--Source manipulation
    private boolean isAtEnd() { //call to ensure EOF safety
        return next >= source.length();
    } //EOF safe

    private char advance() { //not EOF safe
        char c = source.charAt(next++);
        column++;
        if (c == '\n') {
            line++;
            column = 0;
        }
        return c;
    }

    private char peek(int steps) { //EOF safe
        if (next + steps - 1 >= source.length()) return '\0';
        return source.charAt(next + steps - 1);
    }

    private boolean match(char expected) { //EOF safe
        return !isAtEnd() && peek(1) == expected && advance() == expected;
    }

    //--Token list management
    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, next);
        tokens.add(new Token(type, lexeme, literal, line, column - lexeme.length() + 1));
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }
}