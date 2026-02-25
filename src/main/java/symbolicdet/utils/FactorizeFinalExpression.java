package symbolicdet.utils;

/**
 * Entry point for symbolic polynomial factorization.
 *
 * <p>Delegates to:
 * <ul>
 *   <li>{@link PythonRunner}     — executes Python scripts
 *   <li>{@link ExpressionConverter} — converts between math notations
 *   <li>{@link HtmlReportWriter} — renders and saves the HTML report
 * </ul>
 *
 * <p>Python scripts are loaded from the {@code scripts/} directory:
 * <ul>
 *   <li>{@code scripts/factorize_terminal.py}
 *   <li>{@code scripts/factorize_latex.py}
 * </ul>
 */
public class FactorizeFinalExpression {

    private static final String TERMINAL_SCRIPT = "scripts/factorize_terminal.py";
    private static final String LATEX_SCRIPT    = "scripts/factorize_latex.py";

    /**
     * Factorizes the given polynomial, writes an HTML report, and returns
     * the human-readable terminal representation.
     *
     * @param polynomial polynomial expression (may use λ or "lambda", ^ for powers)
     * @return factored form in math notation (λ, ·, ^)
     */
    public static String factor(String polynomial, int n) {
        String sympyExpr = ExpressionConverter.toSympyNotation(polynomial);

        String rawTerminal = PythonRunner.run(TERMINAL_SCRIPT, sympyExpr);
        String latexJson   = PythonRunner.run(LATEX_SCRIPT, sympyExpr);

        String terminalOutput = ExpressionConverter.toMathNotation(rawTerminal);

        HtmlReportWriter.write(terminalOutput, latexJson, polynomial, "determinant", n, n);

        return terminalOutput;
    }
}