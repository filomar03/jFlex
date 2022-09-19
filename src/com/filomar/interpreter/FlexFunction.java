package com.filomar.interpreter;

import java.util.List;

public class FlexFunction implements FlexCallable{
    private final Stmt.FunDcl declaration;

    FlexFunction(Stmt.FunDcl declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(interpreter.globals);
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
