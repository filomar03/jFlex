package com.filomar.interpreter;

import java.util.List;

public class FlexFunction implements FlexCallable {
    private final String name;
    private final Expr.Function declaration;
    private final Environment closure;

    FlexFunction(String name, Expr.Function declaration, Environment closure) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
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
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    // Bind 'self' reference to the enclosing env of a function
    FlexFunction bind(FlexInstance instance) {
        Environment instanceReference = new Environment(closure);
        instanceReference.create("self", instance);
        return new FlexFunction(name, declaration, instanceReference);
    }

    @Override
    public String toString() {
        return "[fun " + name + "]";
    }
}
