package symbolicdet.symbolic.parser;

import java.util.*;

public class InfixPostfixConverter {

    public static List<String> convertToPostfix(
            List<String> infixTokens,
            StringBuilder debugLog
    ) {
        Map<String, Integer> operatorPrecedence = new HashMap<>();
        operatorPrecedence.put("+", 1);
        operatorPrecedence.put("-", 1);
        operatorPrecedence.put("*", 2);
        operatorPrecedence.put("/", 2);
        operatorPrecedence.put("^", 3);

        List<String> outputQueue = new ArrayList<>();
        Deque<String> operatorStack = new ArrayDeque<>();

        for (String token : infixTokens) {
            if (token.matches("\\d+\\.\\d+|\\d+|[A-Za-z_][A-Za-z0-9_]*")) {
                outputQueue.add(token);
            } else if ("(".equals(token)) {
                operatorStack.push(token);
            } else if (")".equals(token)) {
                while (!operatorStack.isEmpty() && !"(".equals(operatorStack.peek())) {
                    outputQueue.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty() && "(".equals(operatorStack.peek())) {
                    operatorStack.pop();
                }
            } else {
                while (!operatorStack.isEmpty()
                        && operatorPrecedence.containsKey(operatorStack.peek())
                        && operatorPrecedence.get(operatorStack.peek()) >= operatorPrecedence.get(token)) {
                    outputQueue.add(operatorStack.pop());
                }
                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            outputQueue.add(operatorStack.pop());
        }

        return outputQueue;
    }
}