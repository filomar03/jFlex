package com.filomar.interpreter;

public class FlexClass {
    final String name;

    FlexClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "(class)" + name;
    }
}
