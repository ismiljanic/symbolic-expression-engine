package symbolicdet.matrix;

import symbolicdet.cli.NumericInputs;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.simplify.PolySimplifier;
import symbolicdet.utils.CheckIfNumeric;
import symbolicdet.utils.FactorizeFinalExpression;
import symbolicdet.utils.SymbolicTracer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static symbolicdet.cli.MatrixPrompt.repeatWithReducedMatrix;
import static symbolicdet.cli.NumericInputs.collectBaseInputs;
import static symbolicdet.cli.YesNoPrompt.askYesNo;
import static symbolicdet.matrix.SubstituteNumericMatrix.substituteNumericMatrix;
import static symbolicdet.symbolic.simplify.SimplifySymbolic.simplifySymbolic;
import static symbolicdet.utils.ExpandSymbolic.expandSymbolic;
import static symbolicdet.utils.PopulateTLVariables.populateTLVar;
import static symbolicdet.utils.PopulateXLVariables.populateXLVar;
import static symbolicdet.utils.TraceNumericDeterminant.tracedNumericDeterminant;

public class DeterminantService {
    public static void runDeterminantProcess(Matrix matrix, Set<String> varsUsed, Scanner scanner, SymbolicTracer tracer, Map<String, Double> persistentVars) throws IOException {
        matrix.printMatrix();

        boolean allNumeric = true;
        for (int i = 0; i < matrix.getN() && allNumeric; i++) {
            for (int j = 0; j < matrix.getN() && allNumeric; j++) {
                Symbol symbol = matrix.getData()[i][j];
                if (!CheckIfNumeric.isNumeric(symbol)) {
                    System.out.println("Contains variable/expression: " + symbol.toExpr());
                    allNumeric = false;
                }
            }
        }

        if (!allNumeric) {
            Symbol det = matrix.determinant(tracer, null);
            String expr = det.toExpr();
            System.out.println("\nSymbolic LU determinant:");
            System.out.println(expr);

            String expanded = expandSymbolic(expr);
            String simplified = simplifySymbolic(expanded);
            String expandedPoly = PolySimplifier.expandSymbolicOnly(simplified);
            System.out.println("\nFinal expanded polynomial:");
            System.out.println(expandedPoly);

            System.out.println("\nFactorization options:");
            System.out.println("  1) Factor & group");
            System.out.println("  2) Skip");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();
            if (choice.equals("1")) {
                FactorizeFinalExpression.factor(expandedPoly, matrix.getN());
            }

            if (!askYesNo(scanner, "Calculate numeric determinant with variables?")) return;

            Map<String, Double> variables = new HashMap<>(persistentVars);
            variables.clear();

            List<String> xLVars = varsUsed.stream().filter(v -> v.matches("x_l([+-]\\d+)?")).toList();
            List<String> tLVars = varsUsed.stream().filter(v -> v.matches("t_l([+-]\\d+)?")).toList();
            List<String> lambdaVars = varsUsed.stream()
                    .filter(v -> v.equals("lambda") || v.matches("lambda([+-]\\d+)?"))
                    .toList();

            boolean needXL = !xLVars.isEmpty() || !tLVars.isEmpty();
            boolean needD = varsUsed.contains("d") || !tLVars.isEmpty();
            boolean needLambda = !lambdaVars.isEmpty() || varsUsed.contains("lambda");

            NumericInputs inputs = collectBaseInputs(scanner, needXL, needD, needLambda);
            variables.put("x", inputs.getxValue());
            variables.put("l", inputs.getlValue());
            variables.put("d", inputs.getdValue());
            variables.put("lambda", inputs.getLambdaValue());

            for (String vars : xLVars) if (!variables.containsKey(vars)) populateXLVar(variables, vars, inputs);
            for (String vars : tLVars) if (!variables.containsKey(vars)) populateTLVar(variables, vars, inputs);

            for (String vars : varsUsed)
                if (!variables.containsKey(vars)) {
                    System.out.print("Value for " + vars + ": ");
                    double value = scanner.nextDouble();
                    variables.put(vars, value);
                }

            persistentVars.putAll(variables);

            BigDecimal numericFromSymbolic = det.evalBD(variables);
            System.out.printf("Numeric determinant (symbolic substitution): %.6f%n", numericFromSymbolic);

            BigDecimal[][] numericData = substituteNumericMatrix(matrix, variables);
            boolean detailedNumeric = askYesNo(scanner, "Run numeric LU with these values for correct pivoting?");
            BigDecimal numericDetLU = tracedNumericDeterminant(numericData, tracer, detailedNumeric);
            System.out.printf("Numeric determinant (LU-based): %.6f%n", numericDetLU);
            return;
        } else {
            BigDecimal[][] numericData = new BigDecimal[matrix.getN()][matrix.getN()];
            for (int i = 0; i < matrix.getN(); i++) {
                for (int j = 0; j < matrix.getN(); j++) {
                    numericData[i][j] = BigDecimal.valueOf(matrix.getData()[i][j].eval(new HashMap<>()));
                }
            }
            boolean detailedOutput = askYesNo(scanner, "Detailed step-by-step numeric LU?");
            BigDecimal numericDet = tracedNumericDeterminant(numericData, tracer, detailedOutput);
            System.out.printf("Numeric determinant (LU based): %.6f%n", numericDet);
        }

        repeatWithReducedMatrix(matrix, varsUsed, scanner, tracer, persistentVars, matrix);
    }
}
