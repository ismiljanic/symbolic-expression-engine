package symbolicdet.symbolic.expressions;

import java.math.BigDecimal;
import java.util.Map;

public abstract class Symbol {
    public abstract String toExpr();

    //smaller numbers
    public abstract double eval(Map<String, Double> vars);

    //very big numbers
    public abstract BigDecimal evalBD(Map<String, Double> vars);
}