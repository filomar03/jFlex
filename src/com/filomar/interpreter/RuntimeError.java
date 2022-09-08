package com.filomar.interpreter;

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super();
        this.token = token;
    }
}
