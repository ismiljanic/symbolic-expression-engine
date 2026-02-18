package symbolicdet.symbolic.parser;

import symbolicdet.symbolic.poly.PolyTerm;
import symbolicdet.symbolic.poly.PolynomialOps;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class PostfixPolynomialEvaluator {

    public static List<PolyTerm> evaluatePostfixExpression(
            List<String> postfixTokens,
            StringBuilder debugLog
    ) {
        Deque<List<PolyTerm>> evaluationStack = new ArrayDeque<>();

        for (String token : postfixTokens) {
            if (token.matches("\\d+\\.\\d+|\\d+")) {
                double numericValue = Double.parseDouble(token);
                evaluationStack.push(
                        Collections.singletonList(new PolyTerm(numericValue, 0, 0, 0))
                );
            } else if (token.equalsIgnoreCase("x")) {
                evaluationStack.push(
                        Collections.singletonList(new PolyTerm(1.0, 1, 0, 0))
                );
            } else if (token.equalsIgnoreCase("l")) {
                evaluationStack.push(
                        Collections.singletonList(new PolyTerm(1.0, 0, 1, 0))
                );
            } else if (token.equalsIgnoreCase("d")) {
                evaluationStack.push(
                        Collections.singletonList(new PolyTerm(1.0, 0, 0, 1))
                );
            } else if (token.equals("+") || token.equals("-")
                    || token.equals("*") || token.equals("/")) {

                List<PolyTerm> rightOperand = evaluationStack.pop();
                List<PolyTerm> leftOperand = evaluationStack.pop();
                List<PolyTerm> operationResult;

                switch (token) {
                    case "+":
                        operationResult = PolynomialOps.add(leftOperand, rightOperand);
                        break;
                    case "-":
                        operationResult = PolynomialOps.add(
                                leftOperand,
                                PolynomialOps.negate(rightOperand)
                        );
                        break;
                    case "*":
                        operationResult = PolynomialOps.multiply(leftOperand, rightOperand);
                        break;
                    case "/":
                        if (rightOperand.size() == 1 && leftOperand.size() == 1
                                && rightOperand.get(0).xx == 0
                                && rightOperand.get(0).lx == 0
                                && rightOperand.get(0).dx == 0) {

                            BigDecimal dividedCoefficient = getBigDecimal(rightOperand, leftOperand);

                            operationResult = Collections.singletonList(
                                    new PolyTerm(
                                            dividedCoefficient,
                                            leftOperand.get(0).xx,
                                            leftOperand.get(0).lx,
                                            leftOperand.get(0).dx
                                    )
                            );
                        } else {
                            List<PolyTerm> invertedDivisor = new ArrayList<>();

                            for (PolyTerm term : rightOperand) {
                                if (term.coeff.compareTo(BigDecimal.ZERO) == 0) {
                                    throw new ArithmeticException(
                                            "Division by zero in symbolic inversion"
                                    );
                                }
                                BigDecimal invertedCoefficient =
                                        BigDecimal.ONE.divide(
                                                term.coeff,
                                                MathContext.DECIMAL128
                                        );
                                invertedDivisor.add(
                                        new PolyTerm(
                                                invertedCoefficient,
                                                -term.xx,
                                                -term.lx,
                                                -term.dx
                                        )
                                );
                            }
                            operationResult =
                                    PolynomialOps.multiply(leftOperand, invertedDivisor);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unsupported operator: " + token);
                }
                evaluationStack.push(operationResult);
            } else {
                throw new IllegalArgumentException("Unexpected token: " + token);
            }
        }
        return PolynomialOps.combine(evaluationStack.pop());
    }

    private static BigDecimal getBigDecimal(List<PolyTerm> rightOperand, List<PolyTerm> leftOperand) {
        BigDecimal divisorCoefficient = rightOperand.get(0).coeff;
        if (divisorCoefficient.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }

        return leftOperand.get(0).coeff.divide(
                divisorCoefficient,
                MathContext.DECIMAL128
        );
    }
}