package symbolicdet.utils;

public class ExpandSymbolic {
    public static String expandSymbolic(String expression) {
        // handle x_l±n — wrap all replacements in parentheses so outer multipliers apply to the whole
        expression = expression.replaceAll("x_l\\+?(\\d+)", "(x - 2*((l+$1)-1)*(l+$1))");
        expression = expression.replaceAll("x_l-(\\d+)", "(x - 2*((l-$1)-1)*(l-$1))");
        expression = expression.replaceAll("x_l(?![+-])", "(x - 2*(l - 1)*l)");

        // handle t_l±n — same rule: always wrap fully in parentheses
        expression = expression.replaceAll("t_l\\+?(\\d+)", "((x - 2*((l+$1)-1)*(l+$1)) * (x - 2*((l+$1+1)-1)*(l+$1+1)) - d)");
        expression = expression.replaceAll("t_l-(\\d+)", "((x - 2*((l-$1)-1)*(l-$1)) * (x - 2*((l-$1+1)-1)*(l-$1+1)) - d)");
        expression = expression.replaceAll("t_l(?![+-])", "((x - 2*(l - 1)*l) * (x - 2*((l+1)-1)*(l+1)) - d)");

        // normalize redundant operators
        expression = expression.replaceAll("--", "+");
        expression = expression.replaceAll("\\+\\+", "+");
        expression = expression.replaceAll("\\+-", "-");
        expression = expression.replaceAll("-\\+", "-");

        return expression;
    }
}
