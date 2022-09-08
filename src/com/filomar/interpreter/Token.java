package com.filomar.interpreter;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;
    final int column;

    Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}
