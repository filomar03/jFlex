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
        FlexFunction init = findMethod("init");
        if (init != null) return init.arity();
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        FlexInstance instance = new FlexInstance(this);
        FlexFunction init = findMethod("init");
        if (init != null) init.bind(instance).call(interpreter, arguments);
        return instance;
    }

    FlexFunction findMethod(String name) {
        return methods.get(name);
    }

    @Override
    public String toString() {
        return "[class " + name + "]";
    }
}
