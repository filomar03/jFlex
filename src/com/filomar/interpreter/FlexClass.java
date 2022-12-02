package com.filomar.interpreter;

import java.util.List;
import java.util.Map;

public class FlexClass implements FlexCallable {
    final String name;
    private final FlexClass superClass;
    private final Map<String, FlexFunction> methods;

    FlexClass(String name, FlexClass superClass, Map<String, FlexFunction> methods) {
        this.name = name;
        this.superClass = superClass;
        this.methods = methods;
    }

    // FlexCallable interface implementation
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

    // Select class method
    FlexFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superClass != null) {
            return superClass.findMethod(name);
        }

        return null;
    }


    @Override
    public String toString() {
        return "[class " + name + "]";
    }
}
