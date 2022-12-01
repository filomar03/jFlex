package com.filomar.interpreter;

import java.util.List;

public class FlexFunction implements FlexCallable {
    private final String name;
    private final Expr.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    FlexFunction(String name, Expr.Function declaration, Environment closure, boolean isInitializer) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    // FlexCallable interface implementation
    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(this.closure);
        for (int i = 0; i < declaration.parameters.size(); i++) {
            environment.create(declaration.parameters.get(i).lexeme(), arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnEx) {
            if (isInitializer) return closure.getAt("self", 0);
            return returnEx.value;
        }
        if (isInitializer) return closure.getAt("self", 0);
        return null;
    }

    FlexFunction bind(FlexInstance instance) {
        Environment instanceReference = new Environment(closure);
        instanceReference.create("self", instance);
        return new FlexFunction(name, declaration, instanceReference, isInitializer);
    }

    @Override
    public String toString() {
        return "[fun " + name + "]";
    }
}
