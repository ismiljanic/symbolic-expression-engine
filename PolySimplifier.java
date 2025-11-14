import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.regex.*;

public class PolySimplifier {

    private static class PolyTerm {
        BigDecimal coeff;
        int xx, lx, dx; // exponents

        PolyTerm(BigDecimal coeff, int xx, int lx, int dx) {
            this.coeff = coeff;
            this.xx = xx;
            this.lx = lx;
            this.dx = dx;
        }

        PolyTerm(double coeff, int xx, int lx, int dx) {
            this(BigDecimal.valueOf(coeff), xx, lx, dx);
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "coef=%s, x^%d l^%d d^%d",
                    coeff.toPlainString(), xx, lx, dx);
        }
    }

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
                        res = add(a, b);
//                        log.append("  add -> ").append(res).append("\n");
                        break;
                    case "-":
                        res = add(a, negate(b));
//                        log.append("  sub -> ").append(res).append("\n");
                        break;
                    case "*":
                        res = multiply(a, b);
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
                            res = multiply(a, inverted);
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
        return combine(stack.pop());
    }

    private static List<PolyTerm> negate(List<PolyTerm> list) {
        List<PolyTerm> r = new ArrayList<>();
        for (PolyTerm t : list)
            r.add(new PolyTerm(t.coeff.negate(), t.xx, t.lx, t.dx));
        return r;
    }


    private static List<PolyTerm> add(List<PolyTerm> a, List<PolyTerm> b) {
        List<PolyTerm> out = new ArrayList<>(a);
        out.addAll(b);
        return combine(out);
    }

    private static List<PolyTerm> multiply(List<PolyTerm> a, List<PolyTerm> b) {
        List<PolyTerm> res = new ArrayList<>();
        for (PolyTerm A : a)
            for (PolyTerm B : b)
                res.add(new PolyTerm(
                        A.coeff.multiply(B.coeff, MathContext.DECIMAL128),
                        A.xx + B.xx,
                        A.lx + B.lx,
                        A.dx + B.dx));
        return combine(res);
    }

    private static List<PolyTerm> combine(List<PolyTerm> list) {
        Map<String, BigDecimal> map = new HashMap<>();

        for (PolyTerm t : list) {
            String key = t.xx + "," + t.lx + "," + t.dx;
            map.put(key, map.getOrDefault(key, BigDecimal.ZERO)
                    .add(t.coeff, MathContext.DECIMAL128));
        }

        List<PolyTerm> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            if (e.getValue().abs().compareTo(new BigDecimal("1e-4")) < 0)
                continue; // treat as zero

            String[] p = e.getKey().split(",");
            int xx = Integer.parseInt(p[0]);
            int lx = Integer.parseInt(p[1]);
            int dx = Integer.parseInt(p[2]);

            out.add(new PolyTerm(e.getValue(), xx, lx, dx));
        }

        return out;
    }


    private static String formatTerms(List<PolyTerm> terms) {
        if (terms.isEmpty()) return "0";

        // Sort by exponents: x > l > D
        terms.sort((a, b) -> {
            int c = Integer.compare(b.xx, a.xx);
            if (c != 0) return c;
            c = Integer.compare(b.lx, a.lx);
            if (c != 0) return c;
            return Integer.compare(b.dx, a.dx);
        });

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (PolyTerm t : terms) {
            // Skip near-zero coefficients
            if (t.coeff.abs().compareTo(new BigDecimal("1e-4")) < 0) continue;

            BigDecimal c = t.coeff;
            boolean negative = c.signum() < 0;
            BigDecimal abs = c.abs();

            boolean needCoeff = !(abs.compareTo(BigDecimal.ONE) == 0 && (t.xx + t.lx + t.dx) > 0);

            // Append sign
            if (!first) {
                sb.append(negative ? " - " : " + ");
            } else if (negative) {
                sb.append("-");
            }
            first = false;

            // Append coefficient
            if (needCoeff) {
                // If integer, print without decimal point
                if (abs.stripTrailingZeros().scale() <= 0) {
                    sb.append(abs.toBigIntegerExact());
                } else {
                    // Otherwise, print with minimal decimal places
                    sb.append(abs.stripTrailingZeros().toPlainString());
                }
            }

            // Append variables
            if (t.xx > 0) {
                sb.append(needCoeff ? "*" : "").append("x");
                if (t.xx > 1) sb.append("^").append(t.xx);
            }
            if (t.lx > 0) {
                sb.append((t.xx > 0 || needCoeff) ? "*" : "").append("l");
                if (t.lx > 1) sb.append("^").append(t.lx);
            }
            if (t.dx > 0) {
                sb.append((t.xx > 0 || t.lx > 0 || needCoeff) ? "*" : "").append("D");
                if (t.dx > 1) sb.append("^").append(t.dx);
            }
        }

        String out = sb.toString().trim();
        if (out.startsWith("+ ")) out = out.substring(2);
        return out.isEmpty() ? "0" : out;
    }
}