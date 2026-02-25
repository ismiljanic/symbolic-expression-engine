package symbolicdet.utils;

public class ExpressionConverter {

    /**
     * Converts user-facing math notation to SymPy-compatible Python syntax.
     */
    public static String toSympyNotation(String expr) {
        return expr
                .replace("λ", "lam")
                .replace("lambda", "lam")
                .replaceAll("\\^(\\d+)", "**$1");
    }

    /**
     * Converts SymPy/Python terminal output to human-readable math notation.
     * Note: the terminal script now emits λ directly, so no lam→λ replacement needed.
     */
    public static String toMathNotation(String output) {
        return output
                .replaceAll("\\*\\*", "^")
                .replaceAll("(?<=[0-9])\\*(?=[a-zA-Zλ])", "·")
                .replace("*", "·")
                .replace("lam", "λ")
                .replace("lambda", "λ");
    }

    /**
     * Escapes special HTML characters in a string.
     */
    public static String escapeHtml(String s) {
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}