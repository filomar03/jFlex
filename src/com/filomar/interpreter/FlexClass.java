package com.filomar.interpreter;

import java.util.List;
import java.util.Map;

public class FlexClass implements FlexCallable {
    private final String name;
    private final Map<String, FlexFunction> methods;


    FlexClass(String name, Map<String, FlexFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
            return new FlexInstance(this);
    }

    FlexFunction findMethod(String name) {
        return methods.get(name);
    }

    @Override
    public String toString() {
        return "[class " + name + "]";
    }
}
