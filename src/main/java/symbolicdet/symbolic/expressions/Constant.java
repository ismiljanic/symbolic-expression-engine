package symbolicdet.symbolic.expressions;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

public class Constant extends Symbol {
    private double value;

   public Constant(double value) {
        this.value = value;
    }

    @Override
    public String toExpr() {
        // if exactly 1.0, don't print decimal form
        if (Math.abs(value - 1.0) < 1e-9) return "1";
        if (Math.abs(value + 1.0) < 1e-9) return "-1";
        // otherwise, print minimal decimal form
        return String.format(Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public double eval(Map<String, Double> vars) {
        return value;
    }

    @Override
    public BigDecimal evalBD(Map<String, Double> vars) {
        return BigDecimal.valueOf(value);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}