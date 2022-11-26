package com.filomar.tool;

import java.util.List;
import java.util.Arrays;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.io.IOException;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: GenerateAst <output directory>");
            System.exit(64);
        }
        String packageName = "com.filomar.interpreter";
        defineAst(args[0], packageName, "Expr", Arrays.asList(
                "Assign   : Token target, Expr expression",
                "Set      : Expr object, Token field, Expr value",
                "Logical  : Expr left, Token operator, Expr right",
                "Binary   : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr expression",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Expr object, Token property",
                "Function : List<Token> parameters, List<Stmt> body",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Variable : Token identifier"
                ));
        defineAst(args[0], packageName, "Stmt", Arrays.asList(
                // low-priority statements (aka: declarations)
                "Class      : Token identifier, List<Stmt.Function> methods",
                "Function   : Token identifier, Expr.Function function",
                "Variable   : Token identifier, Expr initializer",
                // high-priority statements (aka: statements)
                "Block      : List<Stmt> statements",
                "Break      : Token keyword",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr expression",
                "Return     : Token keyword, Expr expression",
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
        writer.println("\t\t}\n");

        // See AstPrinter.class
        writer.println("\t\t@Override"); // Debug purpose only
        writer.println("\t\tpublic String toString() {"); // Debug purpose only
        writer.println("\t\t\treturn Flex.getAstPrinter().stringify(this);"); // Debug purpose only
        writer.println("\t\t}"); // Debug purpose only

        writer.println("\t}\n"); 
    }
}
