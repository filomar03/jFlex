package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment parent;
    private final Map<String, Object> bindings = new HashMap<>();

    Environment() {
        parent = null;
    }

    Environment(Environment enclosure) {
        this.parent = enclosure;
    }

    void newBinding(String name, Object value) {
        bindings.put(name, value);
    }

    Object getValue(Token identifier) {
        if (bindings.containsKey(identifier.lexeme))
            return bindings.get(identifier.lexeme);

        if (parent != null)
            return parent.getValue(identifier);

        throw new RuntimeError(identifier, "Undefined variable '" + identifier.lexeme + "'.");
    }

    void setValue(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme)) {
            bindings.put(identifier.lexeme, value);
            return;
        }

        if (parent != null) {
            parent.setValue(identifier, value);
            return;
        }

        throw new RuntimeError(identifier, "Undefined variable " + identifier.lexeme + "'.");
    }
}
