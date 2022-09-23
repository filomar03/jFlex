package com.filomar.interpreter;

import java.util.List;

public class FlexFunction implements FlexCallable{
    private final String name;
    private final Expr.Function declaration;
    private final Environment closure;

    FlexFunction(String name, Expr.Function declaration, Environment environment) {
        this.name = name;
        this.declaration = declaration;
        this.closure = environment;
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(this.closure);
        for (int i = 0; i < declaration.parameters.size(); i++) {
            environment.createBinding(declaration.parameters.get(i), arguments.get(i));
        }
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    @Override
    public String toString() {
        return "<" + name + " fun>";
    }
}
