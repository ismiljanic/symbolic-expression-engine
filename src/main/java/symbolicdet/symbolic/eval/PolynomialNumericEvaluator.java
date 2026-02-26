package symbolicdet.symbolic.eval;

import symbolicdet.symbolic.poly.PolyTerm;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class PolynomialNumericEvaluator {

    private static BigDecimal computePower(BigDecimal base, int exponent) {
        return base.pow(exponent, MathContext.DECIMAL128);
    }

    public static BigDecimal evaluatePolynomial(
            List<PolyTerm> terms,
            BigDecimal xValue, BigDecimal lValue,
            BigDecimal dValue, BigDecimal lambdaValue) {

        BigDecimal result = BigDecimal.ZERO;
        for (PolyTerm term : terms) {
            BigDecimal evaluatedTerm = term.coeff
                    .multiply(computePower(xValue, term.xx), MathContext.DECIMAL128)
                    .multiply(computePower(lValue, term.lx), MathContext.DECIMAL128)
                    .multiply(computePower(dValue, term.dx), MathContext.DECIMAL128)
                    .multiply(computePower(lambdaValue, term.lambdax), MathContext.DECIMAL128);
            result = result.add(evaluatedTerm, MathContext.DECIMAL128);
        }
        return result;
    }
}