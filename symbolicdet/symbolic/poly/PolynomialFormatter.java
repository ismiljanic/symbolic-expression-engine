package symbolicdet.symbolic.poly;

import java.math.BigDecimal;
import java.util.*;

public final class PolynomialFormatter {

    private PolynomialFormatter() {
    }

    public static String formatTerms(List<PolyTerm> terms) {
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
                sb.append((t.xx > 0 || t.lx > 0 || needCoeff) ? "*" : "").append("d");
                if (t.dx > 1) sb.append("^").append(t.dx);
            }
            if (t.lambdax > 0) {
                sb.append((t.xx > 0 || t.lx > 0 || t.dx > 0 || needCoeff) ? "*" : "")
                        .append("λ");
                if (t.lambdax > 1) sb.append("^").append(t.lambdax);
        }
    }

    String out = sb.toString().trim();
        if(out.startsWith("+ "))out =out.substring(2);
        return out.isEmpty()?"0":out;
}
}
