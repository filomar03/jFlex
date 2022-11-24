package com.filomar.interpreter;

public enum TokenType {
    //Single character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, SEMICOLON, MINUS, MODULUS, PLUS, SLASH, STAR,

    //One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    //Literals.
    IDENTIFIER, STRING, NUMBER,

    //Keywords.
    AND, BREAK, CLASS, ELSE, FALSE, FUN, FOR, IF, NULL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    //Special
    EOF
}
