package com.filomar.interpreter;

public record Token (TokenType type, String lexeme, Object literal, int line, int column) { //update to a record
    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    public TokenType type() {
        return type;
    }

    @Override
    public String lexeme() {
        return lexeme;
    }

    @Override
    public Object literal() {
        return literal;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public int column() {
        return column;
    }
}
