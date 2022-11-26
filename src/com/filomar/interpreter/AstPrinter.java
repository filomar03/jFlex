/*
    This class stringify AbstractSyntaxTree nodes expanding in a tree layout

    Override toString() methods of all Expr.class and Stmt.class subclasses with the following to gather useful string when using the debugger.
    Example:
        @Override
        public String toString() {
            return Flex.debugAstPrinter().stringify(this);
        }
*/

package com.filomar.interpreter;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.stream.IntStream;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    private static class AstStringFormatter {
        private static final String trunk = "|";
        private static final String branch = "--";
        private static final String nodePrefix = trunk.concat(branch);

        static String generatePrefix(String attribute, boolean goingBelow) {
            return (goingBelow ? "|" : " ") + " ".repeat(nodePrefix.length() + attribute.length() + 1);
        }

        static String applyPrefix(String source, String prefix) {
            return source.replace("\n", "\n".concat(prefix));
        }

        static String formatBaseNode(String nodeName, String nodeValue) {
            return nodeName + ": " + nodeValue;
        }

        static String formatCompoundNode(String nodeName, List<String> nodeAttributes, List<String> nodeValues) {
            StringBuilder builder = new StringBuilder();

            Iterator<String> attributesIterator = nodeAttributes.iterator();
            Iterator<String> valuesIterator = nodeValues.iterator();

            builder.append(nodeName).append(":");
            while (attributesIterator.hasNext() && valuesIterator.hasNext()) {
                String currentAttribute = attributesIterator.next();
                String currentValue = valuesIterator.next();
                currentValue = applyPrefix(currentValue, generatePrefix(currentAttribute, valuesIterator.hasNext()));
                
                builder.append("\n").append(nodePrefix).append(currentAttribute).append(": ").append(currentValue);
            }

            return builder.toString();
        }
    }

    // Visitor pattern type matching
    String stringify(Expr expr) {
        return expr.accept(this);
    }

    String stringify(Stmt stmt) {
        return stmt.accept(this);
    }

    // Helper methods to stringify objects that don't require visitor pattern type matching
    String stringify(Token token) {
        return token.lexeme();
    }

    <R> String stringify(List<R> list) {
        return AstStringFormatter.formatCompoundNode("List",
                IntStream.range(0, list.size()).boxed().map(Object::toString).toList(),
                list.stream().map(r -> {
                    if (r instanceof Expr expr) return stringify(expr);
                    else if (r instanceof Stmt stmt) return stringify(stmt);
                    else if (r instanceof Token token) return stringify(token);
                    throw new IllegalArgumentException("Unexpected value: " + r.toString());
                }).toList());
    }

    // Visitor pattern implementations (Expr)
    // --Base cases
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        String value;

        if (expr.value == null) value = "null";
        else if (expr.value instanceof String str) value = '"' + str + '"';
        else if (expr.value instanceof Double n) {
            if (n % 1 == 0) {
                String str = n.toString();
                value = str.substring(0, str.length() - 2);
            } else value = expr.value.toString();
        }
        else value = expr.value.toString();

        return AstStringFormatter.formatBaseNode("Expr.Literal", value);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return AstStringFormatter.formatBaseNode("Expr.Variable", expr.identifier.lexeme());
    }

    // --Non base cases
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Assign",
                Arrays.asList(
                        "target",
                        "expression"
                ),
                Arrays.asList(
                        stringify(expr.target),
                        stringify(expr.expression)
                )
        );
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Set",
                Arrays.asList(
                        "instance",
                        "property",
                        "value"
                ),
                Arrays.asList(
                        stringify(expr.object),
                        stringify(expr.field),
                        stringify(expr.value)
                )
        );
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Logical",
                Arrays.asList(
                        "left",
                        "operator",
                        "right"
                ),
                Arrays.asList(
                        stringify(expr.left),
                        stringify(expr.operator),
                        stringify(expr.right)
                )
        );
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Binary",
                Arrays.asList(
                        "left",
                        "operator",
                        "right"
                ),
                Arrays.asList(
                        stringify(expr.left),
                        stringify(expr.operator),
                        stringify(expr.right)
                )
        );
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Unary",
                Arrays.asList(
                        "operator",
                        "expression"
                ),
                Arrays.asList(
                        stringify(expr.operator),
                        stringify(expr.expression)
                )
        );
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Call",
                Arrays.asList(
                        "callee",
                        "arguments"
                ),
                Arrays.asList(
                        stringify(expr.callee),
                        stringify(expr.arguments)
                )
        );
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Get",
                Arrays.asList(
                        "instance",
                        "property"
                ),
                Arrays.asList(
                        stringify(expr.object),
                        stringify(expr.property)
                )
        );
    }

    @Override
    public String visitFunctionExpr(Expr.Function expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Function",
                Arrays.asList(
                        "parameters",
                        "body"
                ),
                Arrays.asList(
                        stringify(expr.parameters),
                        stringify(expr.body)
                )
        );
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return AstStringFormatter.formatCompoundNode("Expr.Grouping",
                Collections.singletonList(
                        "expression"
                ),
                Collections.singletonList(
                        stringify(expr.expression)
                )
        );
    }

    // Visitor pattern implementations (Stmt)
    // --Base cases
    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return AstStringFormatter.formatBaseNode("Stmt.Break", "null");
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return AstStringFormatter.formatBaseNode("Stmt.Return", stmt.expression != null ?  stringify(stmt.expression) : "null");
    }

    // --Non base cases
    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Class",
                Arrays.asList(
                        "identifier",
                        "methods"
                ),
                Arrays.asList(
                        stringify(stmt.identifier),
                        stringify(stmt.methods)
                )
        );
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Function",
                Arrays.asList(
                        "identifier",
                        "function"
                ),
                Arrays.asList(
                        stringify(stmt.identifier),
                        stringify(stmt.function)
                )
        );
    }

    @Override
    public String visitVariableStmt(Stmt.Variable stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Variable",
                Arrays.asList(
                        "identifier",
                        "initializer"
                ),
                Arrays.asList(
                        stringify(stmt.identifier),
                        stmt.initializer != null ? stringify(stmt.initializer) : "null"
                )
        );
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Block",
                Collections.singletonList(
                        "statements"
                ),
                Collections.singletonList(
                        stringify(stmt.statements)
                )
        );
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.If",
                Arrays.asList(
                        "condition",
                        "thenBranch",
                        "elseBranch"
                ),
                Arrays.asList(
                        stringify(stmt.condition),
                        stringify(stmt.thenBranch),
                        stmt.elseBranch != null ? stringify(stmt.elseBranch) : "null"
                )
        );
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Print",
                Collections.singletonList(
                        "expression"
                ),
                Collections.singletonList(
                        stringify(stmt.expression)
                )
        );
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.While",
                Arrays.asList(
                        "condition",
                        "body"
                ),
                Arrays.asList(
                        stringify(stmt.condition),
                        stringify(stmt.body)
                )
        );
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return AstStringFormatter.formatCompoundNode("Stmt.Expression",
                Collections.singletonList(
                        "expression"
                ),
                Collections.singletonList(
                        stringify(stmt.expression)
                )
        );
    }
}
