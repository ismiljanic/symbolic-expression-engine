package symbolicdet.utils;

import symbolicdet.web.dto.DeterminantResponse.FactorizedGroup;

import java.util.*;

public class FactorizeFinalExpression {

    private static final String TERMINAL_SCRIPT = "scripts/factorize_terminal.py";
    private static final String LATEX_SCRIPT = "scripts/factorize_latex.py";

    public static List<FactorizedGroup> factor(String polynomial, int n) {
        String sympyExpr = ExpressionConverter.toSympyNotation(polynomial);

        String rawTerminal = PythonRunner.run(TERMINAL_SCRIPT, sympyExpr);
        String latexJson = PythonRunner.run(LATEX_SCRIPT, sympyExpr);
        String terminalOutput = ExpressionConverter.toMathNotation(rawTerminal);
        System.out.println(terminalOutput);
        HtmlReportWriter.write(terminalOutput, latexJson, polynomial, "determinant", n, n);
        return parseFactorizedOutput(terminalOutput);
    }

    private static List<FactorizedGroup> parseFactorizedOutput(String output) {
        List<FactorizedGroup> groups = new ArrayList<>();
        if (output == null || output.isBlank()) return groups;

        Map<String, List<String>> lambdaMap = new LinkedHashMap<>();

        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.equals("det =")) continue;

            String factor = "1";
            String inner = line;  // By default, inner = whole term

            // Detect lambda factor
            int lambdaIdx = line.indexOf("λ");
            if (lambdaIdx >= 0) {
                // Find the full lambda factor (e.g., λ^3)
                int endIdx = lambdaIdx + 1;
                while (endIdx < line.length() && (Character.isDigit(line.charAt(endIdx)) || line.charAt(endIdx) == '^')) {
                    endIdx++;
                }
                factor = line.substring(lambdaIdx, endIdx).trim();

                // Inner = full term (factor stays included)
                inner = line.trim();
            }

            // Remove leading '+'
            if (inner.startsWith("+")) inner = inner.substring(1).trim();
            if (factor.startsWith("+")) factor = factor.substring(1).trim();

            lambdaMap.computeIfAbsent(factor, k -> new ArrayList<>()).add(inner);
        }

        for (Map.Entry<String, List<String>> entry : lambdaMap.entrySet()) {
            groups.add(new FactorizedGroup(entry.getKey(), entry.getValue()));
        }

        return groups;
    }

    private static String cleanTermLine(String line) {
        line = line.replaceAll("^[+\\-]\\s*", "").trim();
        if (line.toLowerCase().startsWith("or ")) {
            return "";
        }
        return line;
    }

    /**
     * Alternative version that keeps the original String return type for backward compatibility.
     */
    public static String factorAsString(String polynomial, int n) {
        String sympyExpr = ExpressionConverter.toSympyNotation(polynomial);

        String rawTerminal = PythonRunner.run(TERMINAL_SCRIPT, sympyExpr);
        String latexJson = PythonRunner.run(LATEX_SCRIPT, sympyExpr);

        String terminalOutput = ExpressionConverter.toMathNotation(rawTerminal);

        HtmlReportWriter.write(terminalOutput, latexJson, polynomial, "determinant", n, n);

        return terminalOutput;
    }
}