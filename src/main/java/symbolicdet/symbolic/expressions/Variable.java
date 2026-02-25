package symbolicdet.symbolic.expressions;

import java.math.BigDecimal;
import java.util.Map;

public class Variable extends Symbol {
    private String name;

    public Variable(String name) {
        this.name = name;
    }

    @Override
    public String toExpr() {
        return name;
    }

    @Override
    public double eval(Map<String, Double> vars) {
        if (!vars.containsKey(name)) {
            System.out.println("MISSING: " + name);
            throw new RuntimeException("Missing value for variable: " + name);
        }
        return vars.get(name);
    }

    @Override
    public BigDecimal evalBD(Map<String, Double> vars) {
        if (!vars.containsKey(name))
            throw new RuntimeException("Missing var: " + name);

        return BigDecimal.valueOf(vars.get(name));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}