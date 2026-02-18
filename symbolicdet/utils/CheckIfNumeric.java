package symbolicdet.utils;

import symbolicdet.symbolic.expressions.Constant;
import symbolicdet.symbolic.expressions.Expression;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.expressions.Variable;

public class CheckIfNumeric {
    public static boolean isNumeric(Symbol s) {
        if (s instanceof Constant) return true;
        if (s instanceof Variable) return false;
        if (s instanceof Expression e) return isNumeric(e.getLeft()) && isNumeric(e.getRight());
        return false;
    }
}
