package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    //Fields
    private final Environment parent;
    private final Map<String, Object> bindings = new HashMap<>();

    //Constructors
    Environment() {
        parent = null;
    }

    Environment(Environment enclosure) {
        this.parent = enclosure;
    }

    //Methods
    //--Manage environment bindings
    void createBinding(Token identifier, Object value) {
        bindings.put(identifier.lexeme(), value);
    }

    Object getBinding(Token identifier) {
        if (bindings.containsKey(identifier.lexeme()))
            return bindings.get(identifier.lexeme());

        if (parent != null)
            return parent.getBinding(identifier);

        throw new RuntimeError(identifier, "Undefined binding '" + identifier.lexeme() + "'.");
    }

    void setBinding(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme())) {
            bindings.put(identifier.lexeme(), value);
            return;
        }

        if (parent != null) {
            parent.setBinding(identifier, value);
            return;
        }

        throw new RuntimeError(identifier, "Undefined binding " + identifier.lexeme() + "'.");
    }
}
