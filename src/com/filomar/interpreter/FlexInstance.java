package com.filomar.interpreter;

import java.util.HashMap;
import java.util.Map;

public class FlexInstance {
    private FlexClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    FlexInstance(FlexClass klass) {
        this.klass = klass;
    }

    public Object get(Token property) {
        if (fields.containsKey(property.lexeme())) return fields.get(property.lexeme());
    
        throw new RuntimeError(property, "Desired property doesn't exist in this instance");
    }

    public void set(Token field, Object value) {
        fields.put(field.lexeme(), value);
    }

    @Override
    public String toString() {
        return "(instance)" + klass.name;
    }
}
