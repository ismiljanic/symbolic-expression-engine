package symbolicdet.symbolic.parser;

public final class Preprocessor {

    //    public static String preprocess(String s, StringBuilder log) {
//        String expr = s.replaceAll("\\s+", "");
//        // Insert explicit * between number and letter/paren, and between letter/paren and number/letter/paren
//        expr = expr.replaceAll("(?<=[0-9.])(?=[A-Za-z(])", "*");
//        expr = expr.replaceAll("(?<=[A-Za-z)])(?=[0-9.(A-Za-z])", "*");
//        // Unary minus -> 0- (handles leading - and after operators/()
//        expr = expr.replaceAll("(?<=^|[+\\-*/(])\\-", "0-");
//        return expr;
//    }
    public static String preprocess(String s, StringBuilder log) {
        String expr = s.replaceAll("\\s+", "");
        // number followed by letter or open paren: 2x -> 2*x
        expr = expr.replaceAll("(?<=[0-9.])(?=[A-Za-z(])", "*");
        // closing paren followed by letter, digit, or open paren
        expr = expr.replaceAll("(?<=\\))(?=[0-9A-Za-z(])", "*");
        // letter/digit followed by open paren (but NOT between two letters — that breaks multi-char names)
        expr = expr.replaceAll("(?<=[A-Za-z0-9_])(?=\\()", "*");
        // unary minus
        expr = expr.replaceAll("(^|(?<=[+\\-*/(]))\\-", "0-");
        return expr;
    }
}
