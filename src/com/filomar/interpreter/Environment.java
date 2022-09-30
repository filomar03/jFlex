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
    //--Traverse environments
    Environment getAncestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            assert environment != null : "[ERROR] Cannot find environment specified by resolver";
            environment = environment.parent;
        }

        return environment;
    }

    //--Manage environment bindings
    void create(Token identifier, Object value) {
        bindings.put(identifier.lexeme(), value);
    }

    Object get(Token identifier) {
        if (bindings.containsKey(identifier.lexeme()))
            return bindings.get(identifier.lexeme());

        throw new RuntimeError(identifier, "Undefined binding '" + identifier.lexeme() + "'.");
    }

    Object getAt(String name, int distance) {
        return getAncestor(distance).bindings.get(name);
    }

    void assign(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme())) {
            bindings.put(identifier.lexeme(), value);
            return;
        }

        throw new RuntimeError(identifier, "Undefined binding " + identifier.lexeme() + "'.");
    }

    void assignAt(String name, Object value, int distance) {
        getAncestor(distance).bindings.put(name, value);
    }
}
