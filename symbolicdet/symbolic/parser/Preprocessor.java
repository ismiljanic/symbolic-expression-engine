package symbolicdet.symbolic.parser;

public final class Preprocessor {

    public static String preprocess(String s, StringBuilder log) {
        String expr = s.replaceAll("\\s+", "");
        // Insert explicit * between number and letter/paren, and between letter/paren and number/letter/paren
        expr = expr.replaceAll("(?<=[0-9.])(?=[A-Za-z(])", "*");
        expr = expr.replaceAll("(?<=[A-Za-z)])(?=[0-9.(A-Za-z])", "*");
        // Unary minus -> 0- (handles leading - and after operators/()
        expr = expr.replaceAll("(?<=^|[+\\-*/(])\\-", "0-");
        return expr;
    }
}
