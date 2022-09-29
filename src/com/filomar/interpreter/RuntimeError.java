package com.filomar.interpreter;

public class RuntimeError extends RuntimeException {
    //Fields
    final Token token;

    //Constructors
    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
