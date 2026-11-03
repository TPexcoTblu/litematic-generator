package com.tpexcotblu.litematicgen.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormulaExpression {
    private final List<Token> rpn;
    private final ThreadLocal<double[]> workspace;

    public FormulaExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Formula cannot be empty");
        }
        this.rpn = toRpn(tokenize(expression));
        this.workspace = ThreadLocal.withInitial(() -> new double[rpn.size()]);
        evaluate(0, 0, 0); // Validate variables and operand counts before scanning a volume.
    }

    public double evaluate(double x, double y, double z) {
        double[] stack = workspace.get();
        int size = 0;
        for (Token token : rpn) {
            switch (token.type) {
                case NUMBER -> stack[size++] = token.value;
                case VARIABLE -> stack[size++] = resolveVariable(token.text, x, y, z);
                case OPERATOR -> {
                    if (token.text.equals("neg") || token.text.equals("pos")) {
                        if (size < 1) throw new IllegalArgumentException("Missing unary operand");
                        if (token.text.equals("neg")) stack[size - 1] = -stack[size - 1];
                    } else {
                        if (size < 2) throw new IllegalArgumentException("Missing binary operand");
                        double b = stack[--size], a = stack[size - 1];
                        stack[size - 1] = switch (token.text) {
                            case "+" -> a + b;
                            case "-" -> a - b;
                            case "*" -> a * b;
                            case "/" -> a / b;
                            case "^" -> Math.pow(a, b);
                            default -> throw new IllegalArgumentException("Unknown operator: " + token.text);
                        };
                    }
                }
                case FUNCTION -> {
                    if (size < 1) throw new IllegalArgumentException("Missing function operand");
                    double a = stack[size - 1];
                    stack[size - 1] = switch (token.text) {
                        case "sin" -> Math.sin(a);
                        case "cos" -> Math.cos(a);
                        case "tan" -> Math.tan(a);
                        case "abs" -> Math.abs(a);
                        case "sqrt" -> Math.sqrt(a);
                        case "log" -> Math.log(a);
                        case "exp" -> Math.exp(a);
                        default -> throw new IllegalArgumentException("Unknown function: " + token.text);
                    };
                }
                default -> throw new IllegalArgumentException("Unexpected token");
            }
        }
        if (size != 1) throw new IllegalArgumentException("Formula must reduce to one value");
        return stack[0];
    }

    private static double resolveVariable(String name, double x, double y, double z) {
        return switch (name) {
            case "x" -> x;
            case "y" -> y;
            case "z" -> z;
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            default -> throw new IllegalArgumentException("Unknown variable: " + name);
        };
    }

    private static List<Token> tokenize(String expression) {
        String src = expression.replace(" ", "").toLowerCase(Locale.ROOT);
        List<Token> result = new ArrayList<>();
        int i = 0;

        while (i < src.length()) {
            char c = src.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                i++;
                while (i < src.length() && (Character.isDigit(src.charAt(i)) || src.charAt(i) == '.')) {
                    i++;
                }
                result.add(Token.number(Double.parseDouble(src.substring(start, i))));
                continue;
            }

            if (Character.isAlphabetic(c)) {
                int start = i;
                i++;
                while (i < src.length() && Character.isAlphabetic(src.charAt(i))) {
                    i++;
                }
                String text = src.substring(start, i);
                if (isFunction(text)) {
                    result.add(Token.function(text));
                } else {
                    result.add(Token.variable(text));
                }
                continue;
            }

            if ("+-*/^()".indexOf(c) >= 0) {
                if ((c == '-' || c == '+') && (result.isEmpty() || result.get(result.size() - 1).isOperatorOrLeftParen())) {
                    result.add(Token.operator(c == '-' ? "neg" : "pos"));
                    i++;
                    continue;
                }
                if (c == '(') {
                    result.add(Token.leftParen());
                } else if (c == ')') {
                    result.add(Token.rightParen());
                } else {
                    result.add(Token.operator(String.valueOf(c)));
                }
                i++;
                continue;
            }

            throw new IllegalArgumentException("Unsupported character in formula: '" + c + "'");
        }

        return result;
    }

    private static List<Token> toRpn(List<Token> tokens) {
        List<Token> output = new ArrayList<>();
        List<Token> ops = new ArrayList<>();

        for (Token token : tokens) {
            switch (token.type) {
                case NUMBER, VARIABLE -> output.add(token);
                case FUNCTION -> ops.add(token);
                case OPERATOR -> {
                    while (!ops.isEmpty() && shouldPopBeforePush(ops.get(ops.size() - 1), token)) {
                        output.add(ops.remove(ops.size() - 1));
                    }
                    ops.add(token);
                }
                case LEFT_PAREN -> ops.add(token);
                case RIGHT_PAREN -> {
                    while (!ops.isEmpty() && ops.get(ops.size() - 1).type != TokenType.LEFT_PAREN) {
                        output.add(ops.remove(ops.size() - 1));
                    }
                    if (ops.isEmpty()) {
                        throw new IllegalArgumentException("Mismatched parentheses in formula");
                    }
                    ops.remove(ops.size() - 1);
                    if (!ops.isEmpty() && ops.get(ops.size() - 1).type == TokenType.FUNCTION) {
                        output.add(ops.remove(ops.size() - 1));
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected token while parsing");
            }
        }

        while (!ops.isEmpty()) {
            Token op = ops.remove(ops.size() - 1);
            if (op.type == TokenType.LEFT_PAREN || op.type == TokenType.RIGHT_PAREN) {
                throw new IllegalArgumentException("Mismatched parentheses in formula");
            }
            output.add(op);
        }

        return output;
    }

    private static boolean shouldPopBeforePush(Token top, Token incomingOp) {
        if (incomingOp.text.equals("neg") || incomingOp.text.equals("pos")) return false;
        if (top.type == TokenType.FUNCTION) return true;
        if (top.type != TokenType.OPERATOR) return false;
        int topPrec = precedence(top.text);
        int incomingPrec = precedence(incomingOp.text);
        if (topPrec > incomingPrec) return true;
        return topPrec == incomingPrec && !isRightAssociative(incomingOp.text);
    }

    private static int precedence(String op) {
        return switch (op) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            case "neg", "pos" -> 3;
            case "^" -> 4;
            default -> throw new IllegalArgumentException("Unknown operator precedence for: " + op);
        };
    }

    private static boolean isRightAssociative(String op) {
        return "^".equals(op) || "neg".equals(op) || "pos".equals(op);
    }

    private static boolean isFunction(String text) {
        return switch (text) {
            case "sin", "cos", "tan", "abs", "sqrt", "log", "exp" -> true;
            default -> false;
        };
    }

    private enum TokenType {
        NUMBER, VARIABLE, FUNCTION, OPERATOR, LEFT_PAREN, RIGHT_PAREN
    }

    private static class Token {
        private final TokenType type;
        private final String text;
        private final double value;

        private Token(TokenType type, String text, double value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }

        private static Token number(double value) {
            return new Token(TokenType.NUMBER, null, value);
        }

        private static Token variable(String name) {
            return new Token(TokenType.VARIABLE, name, 0);
        }

        private static Token function(String name) {
            return new Token(TokenType.FUNCTION, name, 0);
        }

        private static Token operator(String op) {
            return new Token(TokenType.OPERATOR, op, 0);
        }

        private static Token leftParen() {
            return new Token(TokenType.LEFT_PAREN, "(", 0);
        }

        private static Token rightParen() {
            return new Token(TokenType.RIGHT_PAREN, ")", 0);
        }

        private boolean isOperatorOrLeftParen() {
            return type == TokenType.OPERATOR || type == TokenType.LEFT_PAREN;
        }
    }
}
