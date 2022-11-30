package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class FlexInstance {
    private final FlexClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    FlexInstance(FlexClass klass) {
        this.klass = klass;
    }

    Object get(Token property) {
        if (fields.containsKey(property.lexeme())) return fields.get(property.lexeme());


        FlexFunction method = klass.findMethod(property.lexeme());
        if (method != null) return method.bind(this);


        throw new RuntimeError(property, "Undefined field '" + property.lexeme() + "'");
    }

    void set(Token field, Object value) {
        fields.put(field.lexeme(), value);
    }

    @Override
    public String toString() {
        return "[instance " + klass + "]@" + hashCode();
    }
}