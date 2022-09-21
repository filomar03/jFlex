package com.filomar.interpreter;

import java.util.List;

public class FlexFunction implements FlexCallable{
    private final Stmt.FunDcl declaration;
    private final Environment closure;

    FlexFunction(Stmt.FunDcl declaration, Environment environment) {
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
            environment.createBinding(declaration.parameters.get(i).lexeme, arguments.get(i));
        }
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    @Override
    public String toString() {
        return "<" + declaration.identifier.lexeme + " fun>";
    }
}
