package com.filomar.tool;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: GenerateAst <output directory>");
            System.exit(64);
        }
        defineAst(args[0], "com.filomar.interpreter", "Expr", Arrays.asList(
                "Assign   : Token identifier, Expr expression",
                "Logical  : Expr left, Token operator, Expr right", //introduced a new class for logical operations to allow short-circuiting without modifying Binary class code
                "Binary   : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr expression",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Literal  : Object value",
                "Variable : Token identifier",
                "Grouping : Expr expression"
                ));
        defineAst(args[0], "com.filomar.interpreter", "Stmt", Arrays.asList(
                //low-priority statements (aka: declarations)
                "FunDcl     : Token identifier, List<Token> parameters, List<Stmt> body",
                "VarDcl     : Token identifier, Expr initializer",
                //high-priority statements (aka: statements)
                "Block      : List<Stmt> statements",
                "Break      : ",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr value",
                "Return     : Expr value",
                "While      : Expr condition, Stmt body",
                "Expression : Expr expression"
                ));
    }

    private static void defineAst(String outputDir, String packageName, String baseClass, List<String> subclasses) throws IOException {
        String path = outputDir + '/' + baseClass + ".java";
        PrintWriter writer = new PrintWriter(path, Charset.defaultCharset());

        writer.println("package " + packageName + ";\n");
        writer.println("import java.util.List;\n");
        writer.println("abstract class " + baseClass + " {");

        defineVisitor(writer, baseClass, subclasses);

        writer.println("\tabstract <R> R accept(Visitor<R> visitor);\n");

        for (String subclass : subclasses) {
            String className = subclass.split(":")[0].trim();
            String fields = subclass.split(":")[1].trim();
            defineSubclass(className, baseClass, fields, writer);
        }

        writer.println("}");

        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\tinterface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t\tR visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("\t}\n");
    }

    private static void defineSubclass(String className, String baseName, String fieldList, PrintWriter writer) {
        writer.println("\tstatic class " + className + " extends " + baseName + " {");

        String[] fields = new String[0];
        if (!fieldList.isEmpty()) {
            fields = fieldList.split(", ");
        }
        for (String field : fields)
            writer.println("\t\tfinal " + field + ";");

        writer.println("\n\t\t" + className + "(" + fieldList + ") {");
        for (String field : fields)
            writer.println("\t\t\tthis." + field.split(" ")[1] + " = " + field.split(" ")[1] + ";");
        writer.println("\t\t}\n");

        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}");

        writer.println("\t}\n");
    }
}
