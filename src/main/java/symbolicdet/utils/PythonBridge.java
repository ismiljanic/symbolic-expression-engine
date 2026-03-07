package symbolicdet.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridges Java symbolic results to Python factorization/evaluation scripts.
 *
 * Usage:
 *   PythonBridge.factorizeTerminal(expandedPoly, "d");   // prints factorized det
 *   PythonBridge.factorizeLatex(expandedPoly);           // prints JSON for LaTeX
 *   PythonBridge.evaluateNumeric(expandedPoly, x, l, d, lam); // prints numeric result
 */
public class PythonBridge {

    // --- Configure paths to your Python scripts here ---
    private static final String PYTHON      = "python3";
    private static final String SCRIPT_DIR  = System.getProperty("scripts.dir", "scripts/python");

    private static final String FACTORIZE_TERMINAL = SCRIPT_DIR + "/factorize_terminal.py";
    private static final String FACTORIZE_LATEX    = SCRIPT_DIR + "/factorize_latex.py";
    private static final String EVALUATE_NUMERIC   = SCRIPT_DIR + "/evaluate_numeric.py";

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Factorize and print to terminal grouped by outerVar (d, x, lam, l).
     */
    public static String factorizeTerminal(String expandedPoly, String outerVar) throws IOException {
        String sympyExpr = toSympyExpr(expandedPoly);
        return runPython(FACTORIZE_TERMINAL, sympyExpr, outerVar);
    }

    /**
     * Factorize and return JSON for LaTeX rendering.
     */
    public static String factorizeLatex(String expandedPoly) throws IOException {
        String sympyExpr = toSympyExpr(expandedPoly);
        return runPython(FACTORIZE_LATEX, sympyExpr);
    }

    /**
     * Evaluate the expanded polynomial numerically using BigDecimal precision.
     */
    public static String evaluateNumeric(
            String expandedPoly,
            String x, String l, String d, String lam
    ) throws IOException {
        String sympyExpr = toSympyExpr(expandedPoly);
        return runPython(EVALUATE_NUMERIC, sympyExpr, x, l, d, lam);
    }

    // -----------------------------------------------------------------------
    // Conversion: Java polynomial string -> SymPy-compatible expression
    // -----------------------------------------------------------------------

    /**
     * Converts the Java polynomial format to a SymPy-compatible string.
     * e.g.  "167400*x^5 - 123120*x^4*λ"  ->  "167400*x**5 - 123120*x**4*lam"
     */
    public static String toSympyExpr(String poly) {
        return poly
                .replace("^", "**")       // x^5 -> x**5
                .replace("λ", "lam")      // λ   -> lam
                .replace("lambda", "lam") // lambda -> lam (just in case)
                .trim();
    }

    // -----------------------------------------------------------------------
    // Process runner
    // -----------------------------------------------------------------------

    private static String runPython(String script, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(PYTHON);
        cmd.add(script);
        for (String arg : args) {
            cmd.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
        }

        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(
                        "Python script failed (exit " + exitCode + "):\n" + stderr
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Python process interrupted", e);
        }

        return stdout.toString().trim();
    }
}