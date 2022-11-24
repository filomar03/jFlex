package com.filomar.interpreter;

public record Token (TokenType type, String lexeme, Object literal, int line, int column) {}
