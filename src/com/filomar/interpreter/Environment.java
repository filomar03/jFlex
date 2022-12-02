package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment closure;
    private final Map<String, Object> bindings = new HashMap<>();

    Environment() {
        closure = null;
    }

    Environment(Environment enclosure) {
        this.closure = enclosure;
    }

    // Traverse environments
    private Environment getAncestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            //assert environment != null; // Debug purpose only
            environment = environment.closure;
        }
        return environment;
    }

    // Manage environment bindings
    void create(String name, Object value) {
        bindings.put(name, value);
    }

    Object get(Token identifier) {
        if (bindings.containsKey(identifier.lexeme()))
            return bindings.get(identifier.lexeme());

        throw new RuntimeError(identifier, "Undefined binding '" + identifier.lexeme() + "'");
    }

    Object getAt(String name, int distance) {
        //assert getAncestor(distance).bindings.containsKey(name); // Debug purpose only
        return getAncestor(distance).bindings.get(name);
    }

    void assign(Token identifier, Object value) {
        if (bindings.containsKey(identifier.lexeme())) {
            bindings.put(identifier.lexeme(), value);
            return;
        }

        throw new RuntimeError(identifier, "Undefined binding " + identifier.lexeme() + "'");
    }

    void assignAt(String name, Object value, int distance) {
        //assert getAncestor(distance).bindings.containsKey(name); // Debug purpose only
        getAncestor(distance).bindings.put(name, value);
    }


    /*public Map<String, Object> getBindings() { // Debug purpose only
        return this.bindings;
    }*/

    /*@Override
    public String toString() { // Debug purpose only
        return Flex.getAstPrinter().stringify(this);
    }*/
}
