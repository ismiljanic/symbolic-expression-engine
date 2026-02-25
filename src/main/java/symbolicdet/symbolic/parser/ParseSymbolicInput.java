package symbolicdet.symbolic.parser;

import symbolicdet.symbolic.expressions.Symbol;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static symbolicdet.symbolic.parser.ExpressionParser.parseExpression;

public class ParseSymbolicInput {
    public static Symbol parseSymbolicInput(String input, Set<String> varsUsed) {
        input = input.replaceAll("\\s+", "");
        AtomicInteger pos = new AtomicInteger(0);
        return parseExpression(input, pos, varsUsed);
    }
}
