package symbolicdet.matrix;

import symbolicdet.cli.NumericInputs;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.simplify.PolySimplifier;
import symbolicdet.utils.*;
import symbolicdet.web.dto.DeterminantResponse.FactorizedGroup;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static symbolicdet.cli.MatrixPrompt.repeatWithReducedMatrix;
import static symbolicdet.cli.NumericInputs.collectBaseInputs;
import static symbolicdet.cli.YesNoPrompt.askYesNo;
import static symbolicdet.matrix.SubstituteNumericMatrix.substituteNumericMatrix;
import static symbolicdet.symbolic.simplify.SimplifySymbolic.simplifySymbolic;
import static symbolicdet.utils.ExpandSymbolic.expandSymbolic;
import static symbolicdet.utils.ExtractVariablesFromMatrix.collectFromSymbolTree;
import static symbolicdet.utils.PopulateTLVariables.populateTLVar;
import static symbolicdet.utils.PopulateXLVariables.populateXLVar;
import static symbolicdet.utils.TraceNumericDeterminant.tracedNumericDeterminant;

public class DeterminantService {

    private static final String EVALUATE_SCRIPT = "scripts/evaluate_numeric.py";

    public static void runDeterminantProcess(
            Matrix matrix,
            Set<String> varsUsed,
            Scanner scanner,
            SymbolicTracer tracer,
            Map<String, Double> persistentVars
    ) throws IOException {

        matrix.printMatrix();
        System.out.println("=== End full symbolic matrix ===");

        boolean allNumeric = true;
        for (int i = 0; i < matrix.getN() && allNumeric; i++) {
            for (int j = 0; j < matrix.getN() && allNumeric; j++) {
                Symbol symbol = matrix.getData()[i][j];
                if (!CheckIfNumeric.isNumeric(symbol)) {
                    allNumeric = false;
                }
            }
        }

        if (!allNumeric) {
            Symbol det = matrix.determinantSmart(tracer);
            String expr = det.toExpr();
            System.out.println("\nSTEP 1 — Determinant obtained from symbolic LU decomposition");
            System.out.println("Structure still contains indexed variables x_{l+n} and auxiliary terms.");
            System.out.println("No substitutions or polynomial expansion have been applied yet.\n");
            System.out.println("Determinant expression:");
            System.out.println(expr);

            String expanded = expandSymbolic(expr);
            String simplified = simplifySymbolic(expanded);
            String expandedPoly = PolySimplifier.expandSymbolicOnly(simplified);

            System.out.println("\nSTEP 2 — Substituting indexed variables x_{l+n}");
            System.out.println("Using definition: x_{l+n} = x - 2(l + n - 1)(l + n)");

            System.out.println("\nSTEP 3 — Fully expanded determinant polynomial");
            System.out.println("All symbolic substitutions have been applied and the determinant");
            System.out.println("is expanded into a multivariate polynomial in x, l, d, and λ.\n");
            System.out.println("Expanded polynomial:");
            System.out.println(expandedPoly);

            // --- Factorization ---
            System.out.println("\nFactorization options:");
            System.out.println("  1) Factor & group");
            System.out.println("  2) Skip");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();
            List<FactorizedGroup> groups = Collections.emptyList();
            if (choice.equals("1")) {
                groups = FactorizeFinalExpression.factor(expandedPoly, matrix.getN());
            }

            if (!askYesNo(scanner, "Calculate numeric determinant with variables?")) return;

            // --- Collect variable values ---
            Map<String, Double> variables = new HashMap<>();
            Set<String> detTreeVars = collectFromSymbolTree(det);
            detTreeVars.addAll(varsUsed);

            List<String> xLVars = detTreeVars.stream()
                    .filter(v -> v.matches("x_l([+-]\\d+)?"))
                    .toList();
            List<String> tLVars = detTreeVars.stream()
                    .filter(v -> v.matches("t_l([+-]\\d+)?"))
                    .toList();

            boolean needXL = !xLVars.isEmpty() || !tLVars.isEmpty()
                    || detTreeVars.contains("x") || detTreeVars.contains("l");
            boolean needD = detTreeVars.contains("d") || !tLVars.isEmpty();
            boolean needLambda = detTreeVars.contains("lambda")
                    || detTreeVars.stream().anyMatch(v -> v.matches("lambda([+-]\\d+)?"));

            NumericInputs inputs = collectBaseInputs(scanner, needXL, needD, needLambda);
            variables.put("x", inputs.getxValue());
            variables.put("l", inputs.getlValue());
            variables.put("d", inputs.getdValue());
            variables.put("lambda", inputs.getLambdaValue());

            for (String v : xLVars) {
                if (!variables.containsKey(v)) populateXLVar(variables, v, inputs);
            }
            for (String v : tLVars) {
                if (!variables.containsKey(v)) populateTLVar(variables, v, inputs);
            }
            for (String v : detTreeVars) {
                if (!variables.containsKey(v)) {
                    System.out.print("Value for " + v + ": ");
                    double value = scanner.nextDouble();
                    scanner.nextLine();
                    variables.put(v, value);
                }
            }

            persistentVars.putAll(variables);
            System.out.println("\nEvaluation parameters used for numeric validation:");
            System.out.println(variables);
            // --- Numeric evaluation: symbolic tree (always correct) ---
            BigDecimal numericFromSymbolic = det.evalBD(variables);
            System.out.println("\nSTEP 5 — Numerical verification of symbolic result");

            System.out.printf(
                    "Determinant value obtained from expanded polynomial (symbolic substitution): %.6f%n",
                    numericFromSymbolic
            );

            // --- Numeric evaluation: Python BigDecimal on expanded polynomial ---
            try {
                String sympyExpr = ExpressionConverter.toSympyNotation(expandedPoly);
                String xVal = formatVar(variables.get("x"));
                String lVal = formatVar(variables.get("l"));
                String dVal = formatVar(variables.get("d"));
                String lamVal = formatVar(variables.get("lambda"));

                String pyResult = PythonRunner.run(EVALUATE_SCRIPT, sympyExpr, xVal, lVal, dVal, lamVal);
                System.out.println(
                        "Determinant value obtained from factorized polynomial (Python BigDecimal evaluation): "
                                + pyResult.trim()
                );

                System.out.println("Verification: both values must match.");
            } catch (Exception e) {
                System.out.println("Python evaluation failed: " + e.getMessage());
            }

            // --- Numeric evaluation: LU on numeric matrix ---
            BigDecimal[][] numericData = substituteNumericMatrix(matrix, variables);
            boolean detailedNumeric = askYesNo(scanner, "Run numeric LU with these values for correct pivoting?");
            BigDecimal numericDetLU = tracedNumericDeterminant(numericData, tracer, detailedNumeric);
            System.out.printf("Numeric determinant (LU-based):              %.6f%n", numericDetLU);
            return;
        } else {
            BigDecimal[][] numericData = new BigDecimal[matrix.getN()][matrix.getN()];
            for (int i = 0; i < matrix.getN(); i++) {
                for (int j = 0; j < matrix.getN(); j++) {
                    numericData[i][j] = BigDecimal.valueOf(
                            matrix.getData()[i][j].eval(new HashMap<>())
                    );
                }
            }
            boolean detailedOutput = askYesNo(scanner, "Detailed step-by-step numeric LU?");
            BigDecimal numericDet = tracedNumericDeterminant(numericData, tracer, detailedOutput);
            System.out.printf("Numeric determinant (LU based): %.6f%n", numericDet);
        }

        repeatWithReducedMatrix(matrix, varsUsed, scanner, tracer, persistentVars, matrix);
    }

    /**
     * Format a Double variable value for passing to Python (no scientific notation).
     */
    private static String formatVar(Double val) {
        if (val == null) return "0";
        if (val == Math.floor(val) && !Double.isInfinite(val)) {
            return String.valueOf(val.longValue());
        }
        return val.toString();
    }
}