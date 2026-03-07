package symbolicdet.utils;

import symbolicdet.web.dto.DeterminantResponse.FactorizedGroup;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FactorizeFinalExpression {

    private static final String TERMINAL_SCRIPT = "scripts/factorize_terminal.py";
    private static final String LATEX_SCRIPT    = "scripts/factorize_latex.py";

    private static final Map<String, String> VAR_CHOICES = new LinkedHashMap<>();
    static {
        VAR_CHOICES.put("1",      "d");
        VAR_CHOICES.put("2",      "x");
        VAR_CHOICES.put("3",      "l");
        VAR_CHOICES.put("4",      "lam");
        VAR_CHOICES.put("d",      "d");
        VAR_CHOICES.put("x",      "x");
        VAR_CHOICES.put("l",      "l");
        VAR_CHOICES.put("lam",    "lam");
        VAR_CHOICES.put("lambda", "lam");
    }

    // ── CLI entry point (prompts user) ────────────────────────────────────────

    public static List<FactorizedGroup> factor(String polynomial, int n) {
        return factorWithVar(polynomial, n, promptVarChoice());
    }

    // ── Web / programmatic entry point ────────────────────────────────────────

    public static List<FactorizedGroup> factorWithVar(String polynomial, int n, String outerVar) {
        String sympyExpr = ExpressionConverter.toSympyNotation(polynomial);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<String> terminalFuture = exec.submit(() ->
                PythonRunner.run(TERMINAL_SCRIPT, sympyExpr, outerVar));
        Future<String> latexFuture = exec.submit(() ->
                PythonRunner.run(LATEX_SCRIPT, sympyExpr, outerVar));

        try {
            String rawTerminal = terminalFuture.get();
            String latexJson   = latexFuture.get();
            exec.shutdown();

            String terminalOutput = ExpressionConverter.toMathNotation(rawTerminal);
            System.out.println(terminalOutput);
            HtmlReportWriter.write(terminalOutput, latexJson, polynomial, "determinant", n, n);
            return parseFactorizedOutput(terminalOutput);
        } catch (Exception e) {
            exec.shutdown();
            throw new RuntimeException("Factorization failed: " + e.getMessage(), e);
        }
    }

    // ── Backward-compat string variant ───────────────────────────────────────

    public static String factorAsString(String polynomial, int n) {
        String outerVar  = promptVarChoice();
        String sympyExpr = ExpressionConverter.toSympyNotation(polynomial);

        String rawTerminal = PythonRunner.run(TERMINAL_SCRIPT, sympyExpr, outerVar);
        String latexJson   = PythonRunner.run(LATEX_SCRIPT, sympyExpr, outerVar);

        String terminalOutput = ExpressionConverter.toMathNotation(rawTerminal);
        HtmlReportWriter.write(terminalOutput, latexJson, polynomial, "determinant", n, n);
        return terminalOutput;
    }

    // ── Prompt (CLI only) ─────────────────────────────────────────────────────

    private static String promptVarChoice() {
        Scanner sc = new Scanner(System.in);
        System.out.println("\nSTEP 4 — Polynomial grouping / factorization");
        System.out.println("Choose the outer variable used to group polynomial terms.");
        System.out.println("This does NOT change the determinant value, only its algebraic presentation.\n");
        System.out.println("  1) d   — constant interaction parameter");
        System.out.println("  2) x   — base variable");
        System.out.println("  3) l   — index parameter");
        System.out.println("  4) λ   — eigenvalue variable");
        System.out.print("\nSelect grouping variable [1–4]: ");

        while (true) {
            String input = sc.nextLine().trim().toLowerCase();
            if (input.equals("lambda")) input = "lam";
            if (VAR_CHOICES.containsKey(input)) return VAR_CHOICES.get(input);
            System.out.print("Invalid choice. Enter 1, 2, 3 or 4: ");
        }
    }

    // ── Parse terminal output → FactorizedGroup list ──────────────────────────

    private static List<FactorizedGroup> parseFactorizedOutput(String output) {
        List<FactorizedGroup> groups = new ArrayList<>();
        if (output == null || output.isBlank()) return groups;

        Map<String, List<String>> map = new LinkedHashMap<>();

        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.equals("det =")) continue;

            String factor = "1";
            String inner  = line;

            int lambdaIdx = line.indexOf("λ");
            if (lambdaIdx >= 0) {
                int end = lambdaIdx + 1;
                while (end < line.length() &&
                        (Character.isDigit(line.charAt(end)) || line.charAt(end) == '^')) end++;
                factor = line.substring(lambdaIdx, end).trim();
                inner  = line.trim();
            }

            if (inner.startsWith("+"))  inner  = inner.substring(1).trim();
            if (factor.startsWith("+")) factor = factor.substring(1).trim();

            map.computeIfAbsent(factor, k -> new ArrayList<>()).add(inner);
        }

        for (Map.Entry<String, List<String>> e : map.entrySet())
            groups.add(new FactorizedGroup(e.getKey(), e.getValue()));

        return groups;
    }
}