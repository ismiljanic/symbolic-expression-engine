package symbolicdet.symbolic.poly;

import java.math.BigDecimal;
import java.util.Locale;

public final class PolyTerm {
    public BigDecimal coeff;
    public int xx, lx, dx;

    public PolyTerm(BigDecimal coeff, int xx, int lx, int dx) {
        this.coeff = coeff;
        this.xx = xx;
        this.lx = lx;
        this.dx = dx;
    }

    public PolyTerm(double coeff, int xx, int lx, int dx) {
        this(BigDecimal.valueOf(coeff), xx, lx, dx);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "coef=%s, x^%d l^%d d^%d",
                coeff.toPlainString(), xx, lx, dx
        );
    }
}
