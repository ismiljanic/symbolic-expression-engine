package symbolicdet.symbolic.simplify;

public class SimplifySymbolic {
    public static String simplifySymbolic(String expression) {
        expression = expression.replaceAll("\\s+", ""); // remove spaces

        // Replace ^ with Math.pow
        expression = expression.replaceAll("(\\w+)\\^(\\d+)", "Math.pow($1,$2)");

        // Replace 'l' and 'x' and 'D' with symbols for safety
        // Expand arithmetic patterns like (l+1)-1 → l
        expression = expression.replaceAll("\\(l\\+1\\)-1", "l");
        expression = expression.replaceAll("\\(l-1\\)\\+1", "l");
        expression = expression.replaceAll("\\(l\\+0\\)", "l");
        expression = expression.replaceAll("\\(l-0\\)", "l");

        // Expand (l+1)*(l+1) → l*l + 2*l + 1
        expression = expression.replaceAll("\\(l\\+1\\)\\*(l\\+1\\))", "(l*l + 2*l + 1)");
        expression = expression.replaceAll("\\(l-1\\)\\*(l-1\\))", "(l*l - 2*l + 1)");
        expression = expression.replaceAll("\\(l\\+1\\)\\*(l-1\\))", "(l*l - 1)");
        expression = expression.replaceAll("\\(l-1\\)\\*(l\\+1\\))", "(l*l - 1)");


        // Expand nested (l+n)-1 and (l-n)-1 patterns generally
        expression = expression.replaceAll("\\(l\\+([0-9]+)\\)-1", "(l+$1-1)");
        expression = expression.replaceAll("\\(l-([0-9]+)\\)-1", "(l-$1-1)");

        // Simplify *1, 1*, etc.
        expression = expression.replaceAll("\\*1(?!\\d)", "");
        expression = expression.replaceAll("1\\*", "");

        // Simplify double negatives
        expression = expression.replaceAll("--", "+");
        expression = expression.replaceAll("\\+-", "-");
        expression = expression.replaceAll("-\\+", "-");

        return expression;
    }
}
