package com.filomar.interpreter;

import java.util.List;

public interface FlexCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
