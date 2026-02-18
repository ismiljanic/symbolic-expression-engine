import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.regex.*;
import symbolicdet.symbolic.poly.*;

public class PolySimplifier {

    private static BigDecimal powBD(BigDecimal base, int exp) {
        return base.pow(exp, MathContext.DECIMAL128);
    }

    public static BigDecimal evaluatePolyTerms(
            List<PolyTerm> terms,
            BigDecimal xVal,
            BigDecimal lVal,
            BigDecimal DVal) {

        BigDecimal result = BigDecimal.ZERO;

        for (PolyTerm t : terms) {
            BigDecimal term = t.coeff
                    .multiply(powBD(xVal, t.xx), MathContext.DECIMAL128)
                    .multiply(powBD(lVal, t.lx), MathContext.DECIMAL128)
                    .multiply(powBD(DVal, t.dx), MathContext.DECIMAL128);

            result = result.add(term, MathContext.DECIMAL128);
        }

        return result;
    }

    public static String expandSymbolicOnly(String rawExpr) {
        StringBuilder log = new StringBuilder();

        String expr = preprocess(rawExpr, log);
        List<String> tokens = tokenize(expr, log);
        List<String> postfix = infixToPostfix(tokens, log);

        List<PolyTerm> terms;
        try {
            terms = evalPostfix(postfix, log);
        } catch (Exception ex) {
            log.append("ERROR during evalPostfix: ").append(ex.getMessage()).append("\n");
            return log.toString();
        }

        return formatTerms(terms); // just return the symbolic polynomial string
    }

    public static BigDecimal evaluateNumeric(String rawExpr, BigDecimal xVal, BigDecimal lVal, BigDecimal DVal) {
        StringBuilder log = new StringBuilder();

        String expr = preprocess(rawExpr, log);
        List<String> tokens = tokenize(expr, log);
        List<String> postfix = infixToPostfix(tokens, log);

        List<PolyTerm> terms = evalPostfix(postfix, log);
        return evaluatePolyTerms(terms, xVal, lVal, DVal);
    }

//    public static String expandWithDebug(String rawExpr, BigDecimal xVal, BigDecimal lVal, BigDecimal DVal) {
//        StringBuilder log = new StringBuilder();
//
//        String expr = preprocess(rawExpr, log);
//        List<String> tokens = tokenize(expr, log);
//        List<String> postfix = infixToPostfix(tokens, log);
//
//        List<PolyTerm> terms;
//        try {
//            terms = evalPostfix(postfix, log);
//        } catch (Exception ex) {
//            log.append("ERROR during evalPostfix: ").append(ex.getMessage()).append("\n");
//            return log.toString();
//        }
//
//        String finalStr = formatTerms(terms);
//        log.append("\nFinal expanded polynomial:\n").append(finalStr).append("\n");
//
//        if (xVal != null && lVal != null && DVal != null) {
//            BigDecimal numeric = evaluatePolyTerms(terms, xVal, lVal, DVal);
//            log.append("Numeric evaluation in expandWithDebug: ")
//                    .append(numeric.toPlainString())
//                    .append(" for xVal=")
//                    .append(xVal)
//                    .append(" lVal=")
//                    .append(lVal)
//                    .append(" DVal=")
//                    .append(DVal)
//                    .append("\n");
//        }
//
//        return log.toString();
//    }

    private static String preprocess(String s, StringBuilder log) {
        String expr = s.replaceAll("\\s+", "");
        // Insert explicit * between number and letter/paren, and between letter/paren and number/letter/paren
        expr = expr.replaceAll("(?<=[0-9.])(?=[A-Za-z(])", "*");
        expr = expr.replaceAll("(?<=[A-Za-z)])(?=[0-9.(A-Za-z])", "*");
        // Unary minus -> 0- (handles leading - and after operators/()
        expr = expr.replaceAll("(?<=^|[+\\-*/(])\\-", "0-");
        return expr;
    }

    private static List<String> tokenize(String s, StringBuilder log) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+\\.\\d+|\\d+|[A-Za-z_][A-Za-z0-9_]*|[+\\-*/()^]").matcher(s);
        while (m.find()) tokens.add(m.group());
//        log.append("tokenize: matched ").append(tokens.size()).append(" tokens\n");
        return tokens;
    }

    private static List<String> infixToPostfix(List<String> tokens, StringBuilder log) {
        Map<String, Integer> prec = new HashMap<>();
        prec.put("+", 1);
        prec.put("-", 1);
        prec.put("*", 2);
        prec.put("/", 2);
        prec.put("^", 3);

        List<String> out = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String t : tokens) {
            if (t.matches("\\d+\\.\\d+|\\d+|[A-Za-z_][A-Za-z0-9_]*")) {
                out.add(t);
            } else if ("(".equals(t)) {
                ops.push(t);
            } else if (")".equals(t)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) out.add(ops.pop());
                if (!ops.isEmpty() && "(".equals(ops.peek())) ops.pop();
            } else {
                while (!ops.isEmpty() && prec.containsKey(ops.peek()) && prec.get(ops.peek()) >= prec.get(t))
                    out.add(ops.pop());
                ops.push(t);
            }
//            log.append("shunting step token=").append(t).append(" | out=").append(out).append(" | stack=").append(ops).append("\n");
        }
        while (!ops.isEmpty()) out.add(ops.pop());
//        log.append("final postfix=").append(out).append("\n");
        return out;
    }

    private static List<PolyTerm> evalPostfix(List<String> postfix, StringBuilder log) {
        Deque<List<PolyTerm>> stack = new ArrayDeque<>();
        int step = 0;
        for (String tok : postfix) {
            step++;
//            log.append(String.format("EVAL step %d token=%s\n", step, tok));
            if (tok.matches("\\d+\\.\\d+|\\d+")) {
                double v = Double.parseDouble(tok);
                stack.push(Collections.singletonList(new PolyTerm(v, 0, 0, 0)));
//                log.append("  push number -> ").append(v).append("\n");
            } else if (tok.equalsIgnoreCase("x")) {
                stack.push(Collections.singletonList(new PolyTerm(1.0, 1, 0, 0)));
//                log.append("  push symbol x\n");
            } else if (tok.equalsIgnoreCase("l")) {
                stack.push(Collections.singletonList(new PolyTerm(1.0, 0, 1, 0)));
//                log.append("  push symbol l\n");
            } else if (tok.equalsIgnoreCase("d")) {
                stack.push(Collections.singletonList(new PolyTerm(1.0, 0, 0, 1)));
//                log.append("  push symbol D\n");
            } else if (tok.equals("+") || tok.equals("-") || tok.equals("*") || tok.equals("/")) {
                List<PolyTerm> b = stack.pop();
                List<PolyTerm> a = stack.pop();
//                log.append("  pop A=").append(a).append(" B=").append(b).append("\n");
                List<PolyTerm> res;
                switch (tok) {
                    case "+":
                        res = PolynomialOps.add(a, b);
//                        log.append("  add -> ").append(res).append("\n");
                        break;
                    case "-":
                        res = PolynomialOps.add(a, PolynomialOps.negate(b));
//                        log.append("  sub -> ").append(res).append("\n");
                        break;
                    case "*":
                        res = PolynomialOps.multiply(a, b);
//                        log.append("  mul -> ").append(res).append("\n");
                        break;
                    case "/":
                        if (b.size() == 1 && a.size() == 1 &&
                                b.get(0).xx == 0 && b.get(0).lx == 0 && b.get(0).dx == 0) {
                            // constant division shortcut using BigDecimal
                            BigDecimal denom = b.get(0).coeff;
                            if (denom.compareTo(BigDecimal.ZERO) == 0)
                                throw new ArithmeticException("Division by zero");

                            BigDecimal quotient = a.get(0).coeff.divide(denom, MathContext.DECIMAL128);
                            res = Collections.singletonList(
                                    new PolyTerm(quotient, a.get(0).xx, a.get(0).lx, a.get(0).dx)
                            );
                        } else {
                            // fallback to symbolic exponent inversion with BigDecimal
                            List<PolyTerm> inverted = new ArrayList<>();
                            for (PolyTerm t : b) {
                                if (t.coeff.compareTo(BigDecimal.ZERO) == 0)
                                    throw new ArithmeticException("Division by zero in symbolic inversion");
                                BigDecimal invCoeff = BigDecimal.ONE.divide(t.coeff, MathContext.DECIMAL128);
                                inverted.add(new PolyTerm(invCoeff, -t.xx, -t.lx, -t.dx));
                            }
                            res = PolynomialOps.multiply(a, inverted);
                        }
                        break;

                    default:
                        throw new IllegalStateException("Unknown op");
                }
                stack.push(res);
            } else {
                throw new IllegalArgumentException("Unexpected token " + tok);
            }
//            log.append("  stack now top=").append(stack.peek()).append("\n\n");
        }
        //        log.append("evalPostfix combined -> ").append(out).append("\n");
        return PolynomialOps.combine(stack.pop());
    }
}