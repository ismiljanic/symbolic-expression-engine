package symbolicdet.symbolic.parser;

import symbolicdet.symbolic.expressions.Constant;
import symbolicdet.symbolic.expressions.Expression;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.expressions.Variable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpressionParser {
    public static Symbol parseExpression(String input, AtomicInteger pos, Set<String> varsUsed) {
        Symbol term = parseTerm(input, pos, varsUsed);
        while (pos.get() < input.length()) {
            char op = input.charAt(pos.get());
            if (op != '+' && op != '-') break;
            pos.incrementAndGet();
            Symbol nextTerm = parseTerm(input, pos, varsUsed);
            term = new Expression(term, op, nextTerm);
        }
        return term;
    }

    private static Symbol parseTerm(String input, AtomicInteger pos, Set<String> varsUsed) {
        Symbol factor = parseFactor(input, pos, varsUsed);
        while (pos.get() < input.length()) {
            char op = input.charAt(pos.get());
            if (op != '*' && op != '/') break;
            pos.incrementAndGet();
            Symbol nextFactor = parseFactor(input, pos, varsUsed);
            factor = new Expression(factor, op, nextFactor);
        }
        return factor;
    }

    private static Symbol parseFactor(String input, AtomicInteger pos, Set<String> varsUsed) {
        if (pos.get() >= input.length()) return null;

        char c = input.charAt(pos.get());
        if (c == '(') {
            pos.incrementAndGet();
            Symbol expr = parseExpression(input, pos, varsUsed);
            if (pos.get() >= input.length() || input.charAt(pos.get()) != ')')
                throw new RuntimeException("Mismatched parentheses");
            pos.incrementAndGet();
            return expr;
        }

        if (c == '-') {
            pos.incrementAndGet();
            Symbol inner = parseFactor(input, pos, varsUsed);
            return new Expression(new Constant(-1), '*', inner);
        }

        int start = pos.get();
        while (pos.get() < input.length() && (Character.isLetterOrDigit(input.charAt(pos.get())) || input.charAt(pos.get()) == '_' || input.charAt(pos.get()) == '+' || input.charAt(pos.get()) == '-' || input.charAt(pos.get()) == '.')) {
            pos.incrementAndGet();
        }

        String token = input.substring(start, pos.get());
        if (token.matches("\\d+(\\.\\d+)?")) return new Constant(Double.parseDouble(token.replace(',', '.')));
        varsUsed.add(token);
        return new Variable(token);
    }
}
