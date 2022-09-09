package com.filomar.interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Flex {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            runPrompt();
        else if (args.length == 1)
            runFile(args[0]);
        else {
            System.out.println("Usage: jflex <script>");
            System.exit(64);
        }
    }

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
        Expr expr = parser.parse();

        if (hadError) return;

        interpreter.interpret(expr);
    }

    static void onErrorDetected(int line, int column, String message) {
        notifyError(line, column, message);
        hadError = true;
    }

    static void onRuntimeError(RuntimeError error) {
        notifyError(error.token.line, error.token.column, error.getMessage());
        hadRuntimeError = true;
    }

    private static void notifyError(int line, int column, String message) {
        System.err.println("[" + line + ":" + column + "] ERROR: " + message);
    }
}