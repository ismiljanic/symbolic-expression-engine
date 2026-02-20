package symbolicdet.symbolic.simplify;

import java.math.BigDecimal;
import java.util.*;

import symbolicdet.symbolic.eval.PolynomialNumericEvaluator;
import symbolicdet.symbolic.parser.*;
import symbolicdet.symbolic.poly.*;

public class PolySimplifier {

    public static String expandSymbolicOnly(String rawExpression) {
        StringBuilder debugLog = new StringBuilder();

        String expression = Preprocessor.preprocess(rawExpression, debugLog);
        List<String> tokens = Tokenizer.tokenize(expression, debugLog);
        List<String> postfix = InfixPostfixConverter.convertToPostfix(tokens, debugLog);

        List<PolyTerm> terms;
        try {
            terms = PostfixPolynomialEvaluator.evaluatePostfixExpression(postfix, debugLog);
        } catch (Exception ex) {
            debugLog.append("ERROR during evalPostfix: ").append(ex.getMessage()).append("\n");
            return debugLog.toString();
        }

        return PolynomialFormatter.formatTerms(terms);
    }

    public static BigDecimal evaluateNumeric(String rawExpr, BigDecimal xVal, BigDecimal lVal, BigDecimal DVal, BigDecimal lambdax) {
        StringBuilder log = new StringBuilder();

        String expr = Preprocessor.preprocess(rawExpr, log);
        List<String> tokens = Tokenizer.tokenize(expr, log);
        List<String> postfix = InfixPostfixConverter.convertToPostfix(tokens, log);

        List<PolyTerm> terms = PostfixPolynomialEvaluator.evaluatePostfixExpression(postfix, log);
        return PolynomialNumericEvaluator.evaluatePolynomial(terms, xVal, lVal, DVal, lambdax);
    }
}