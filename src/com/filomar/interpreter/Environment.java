package com.filomar.interpreter;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> bindings = new HashMap<>();

    void newBinding(String name, Object value) {
        bindings.put(name, value);
    }

    Object getValue(Token identifier) {
        if (bindings.containsKey(identifier.lexeme))
            return bindings.get(identifier.lexeme);

        throw new RuntimeError(identifier, "Undefined variable '" + identifier.lexeme + "'.");
    }

    void setValue(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme))
            bindings.put(identifier.lexeme, value);

        throw new RuntimeError(identifier, "Undefined variable '" + identifier.lexeme + "'.");
    }
}
