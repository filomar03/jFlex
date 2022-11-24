package com.filomar.interpreter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class Flex {
    //Fields
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    //Methods
    //--Main
    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            runPrompt();
        else if (args.length == 1)
            runFile(args[0]);
        else {
            System.out.println("Usage: Flex <script>");
            System.exit(64);
        }
    }

    //--Run target program
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) return;

        interpreter.interpret(statements);
    }

    //--Error handling
    static void onErrorDetected(int line, int column, String message) {
        hadError = true;
        notifyError(line, column, message);
    }

    static void onErrorDetected(Token token, String message) {
        hadError = true;
        notifyError(token.line(), token.column(), message);
    }

    static void onRuntimeError(RuntimeError error) {
        hadRuntimeError = true;
        notifyError(error.token.line(), error.token.column(), error.getMessage());
    }

    private static void notifyError(int line, int column, String message) { 
        System.err.println("[" + line + ":" + column + "] ERROR: " + message);
    }

    //Debug utility
    /* public static AstPrinter astPrinter = new AstPrinter();
    public static AstPrinter getDbgAstPrinter() { return astPrinter; } */
}