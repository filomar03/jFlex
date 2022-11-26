package com.filomar.interpreter;

import java.util.List;

public class FlexClass implements FlexCallable {
    final String name;

    FlexClass(String name) {
        this.name = name;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new FlexInstance(this);
    }

    @Override
    public String toString() {
        return "[class " + name + "]";
    }
}
