package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosure;
    private final Map<String, Object> bindings = new HashMap<>();

    Environment() {
        enclosure = null;
    }

    Environment(Environment enclosure) {
        this.enclosure = enclosure;
    }

    void newBinding(String name, Object value) {
        bindings.put(name, value);
    }

    Object getValue(Token identifier) {
        if (bindings.containsKey(identifier.lexeme))
            return bindings.get(identifier.lexeme);

        if (enclosure != null)
            return enclosure.getValue(identifier);

        throw new RuntimeError(identifier, "Undefined variable '" + identifier.lexeme + "'.");
    }

    void setValue(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme)) {
            bindings.put(identifier.lexeme, value);
            return;
        }

        if (enclosure != null) {
            enclosure.setValue(identifier, value);
            return;
        }

        throw new RuntimeError(identifier, "Undefined variable '" + identifier.lexeme + "'.");
    }
}
