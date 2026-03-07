package symbolicdet.web.service;

import org.springframework.stereotype.Service;
import symbolicdet.matrix.Matrix;
import symbolicdet.matrix.SubstituteNumericMatrix;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.simplify.PolySimplifier;
import symbolicdet.utils.CheckIfNumeric;
import symbolicdet.utils.ComputeOffset;
import symbolicdet.utils.ExpandSymbolic;
import symbolicdet.utils.FactorizeFinalExpression;
import symbolicdet.utils.SymbolicTracer;
import symbolicdet.web.dto.DeterminantRequest;
import symbolicdet.web.dto.DeterminantResponse;
import symbolicdet.web.dto.DeterminantResponse.FactorizedGroup;

import java.math.BigDecimal;
import java.util.*;

import static symbolicdet.symbolic.parser.ParseSymbolicInput.parseSymbolicInput;
import static symbolicdet.symbolic.simplify.SimplifySymbolic.simplifySymbolic;
import static symbolicdet.utils.TraceNumericDeterminant.tracedNumericDeterminant;

@Service
public class DeterminantWebService {

    public DeterminantResponse compute(DeterminantRequest req) {
        try {
            return doCompute(req);
        } catch (Exception e) {
            return DeterminantResponse.error("Computation failed: " + e.getMessage());
        }
    }

    private DeterminantResponse doCompute(DeterminantRequest req) {

        // ── Build matrix ─────────────────────────────────────────────────────
        Matrix matrix;
        Set<String> varsUsed = new LinkedHashSet<>();

        if (req.isUseTridiagonal()) {
            // Server-side generation via Matrix.buildSpecialMatrix(n)
            matrix = Matrix.buildSpecialMatrix(req.getTridiagonalSize());
            // Collect variable names from the generated cell expressions
            for (Symbol[] row : matrix.getData()) {
                for (Symbol cell : row) {
                    collectVars(cell.toExpr(), varsUsed);
                }
            }
        } else {
            String[][] raw = req.getMatrix();
            int n = raw.length;
            Symbol[][] data = new Symbol[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (raw[i] == null || j >= raw[i].length
                            || raw[i][j] == null || raw[i][j].isBlank()) {
                        return DeterminantResponse.error(
                                "Missing matrix element at [" + (i + 1) + "," + (j + 1) + "]");
                    }
                    data[i][j] = parseSymbolicInput(raw[i][j].trim(), varsUsed);
                }
            }
            matrix = new Matrix(data);
        }

        // ── Capture symbolic matrix cells for display ────────────────────────
        int matN = matrix.getN();
        String[][] symbolicMatrix = new String[matN][matN];
        for (int i = 0; i < matN; i++)
            for (int j = 0; j < matN; j++)
                symbolicMatrix[i][j] = matrix.getData()[i][j].toExpr();

        // ── Fast path: pure numeric ──────────────────────────────────────────
        if (isPureNumeric(matrix)) {
            DeterminantResponse r = computePureNumeric(matrix, req);
            r.setSymbolicMatrix(symbolicMatrix);
            return r;
        }

        // ── Symbolic path ─────────────────────────────────────────────────────
        List<String> xLVars = varsUsed.stream()
                .filter(v -> v.matches("x_l([+-]\\d+)?")).toList();
        List<String> tLVars = varsUsed.stream()
                .filter(v -> v.matches("t_l([+-]\\d+)?")).toList();
        boolean hasLambda = varsUsed.stream()
                .anyMatch(v -> v.equals("lambda") || v.matches("lambda([+-]\\d+)?"));
        boolean hasD = varsUsed.contains("d");

        DeterminantResponse response = new DeterminantResponse();
        response.setSymbolicMatrix(symbolicMatrix);
        response.setDetectedVariables(new ArrayList<>(varsUsed));
        response.setNeedsXL(!xLVars.isEmpty() || !tLVars.isEmpty());
        response.setNeedsD(hasD || !tLVars.isEmpty());
        response.setNeedsLambda(hasLambda);

        SymbolicTracer tracer = new SymbolicTracer(req.isDetailedLU());

        // Use determinantSmart so tridiagonal matrices use the O(n) recurrence
        Symbol det = matrix.determinantSmart(tracer);
        String expr = det.toExpr();
        response.setSymbolicDeterminant(expr);

        String expanded    = ExpandSymbolic.expandSymbolic(expr);
        String simplified  = simplifySymbolic(expanded);
        String expandedPoly = PolySimplifier.expandSymbolicOnly(simplified);
        response.setExpandedPolynomial(expandedPoly);

        if (req.isDetailedLU()) {
            response.setLuSteps(tracer.getLog());
        }

        // ── Factorization ─────────────────────────────────────────────────────
        if (req.isFactorize()) {
            String outerVar = sanitizeFactorizeVar(req.getFactorizeVar());
            List<FactorizedGroup> factorized =
                    FactorizeFinalExpression.factorWithVar(expandedPoly, matrix.getN(), outerVar);
            response.setFactorized(factorized);
        }

        // ── Numeric evaluation ────────────────────────────────────────────────
        if (req.hasNumericInputs()) {
            try {
                Map<String, Double> variables = buildVariableMap(req, varsUsed, xLVars, tLVars);

                BigDecimal numericFromSymbolic = det.evalBD(variables);
                response.setNumericFromSymbolic(numericFromSymbolic.doubleValue());

                BigDecimal[][] numericData =
                        SubstituteNumericMatrix.substituteNumericMatrix(matrix, variables);
                SymbolicTracer luTracer = new SymbolicTracer(req.isDetailedLU());
                BigDecimal numericDetLU =
                        tracedNumericDeterminant(numericData, luTracer, req.isDetailedLU());
                response.setNumericFromLU(numericDetLU.doubleValue());

                if (req.isDetailedLU()) {
                    List<String> combined = new ArrayList<>(tracer.getLog());
                    combined.addAll(luTracer.getLog());
                    response.setLuSteps(combined);
                }
            } catch (Exception e) {
                response.setError(
                        "Symbolic result computed but numeric evaluation failed: " + e.getMessage());
            }
        }

        return response;
    }

    // ── Pure numeric ──────────────────────────────────────────────────────────

    private DeterminantResponse computePureNumeric(Matrix matrix, DeterminantRequest req) {
        int n = matrix.getN();
        BigDecimal[][] numericData = new BigDecimal[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                numericData[i][j] =
                        BigDecimal.valueOf(matrix.getData()[i][j].eval(Collections.emptyMap()));

        SymbolicTracer tracer = new SymbolicTracer(req.isDetailedLU());
        BigDecimal det = tracedNumericDeterminant(numericData, tracer, req.isDetailedLU());

        DeterminantResponse r = new DeterminantResponse();
        r.setNumericFromLU(det.doubleValue());
        r.setDetectedVariables(Collections.emptyList());
        r.setNeedsXL(false);
        r.setNeedsD(false);
        r.setNeedsLambda(false);
        r.setSymbolicDeterminant(null);
        r.setExpandedPolynomial(null);
        r.setFactorized(Collections.emptyList());
        if (req.isDetailedLU()) r.setLuSteps(tracer.getLog());
        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPureNumeric(Matrix matrix) {
        for (int i = 0; i < matrix.getN(); i++)
            for (int j = 0; j < matrix.getN(); j++)
                if (!CheckIfNumeric.isNumeric(matrix.getData()[i][j])) return false;
        return true;
    }

    /** Pull variable names out of a raw expression string. */
    private void collectVars(String expr, Set<String> out) {
        java.util.regex.Matcher m;
        m = java.util.regex.Pattern.compile("x_l[+-]?\\d*").matcher(expr);
        while (m.find()) out.add(m.group());
        m = java.util.regex.Pattern.compile("t_l[+-]?\\d*").matcher(expr);
        while (m.find()) out.add(m.group());
        if (expr.contains("lambda")) out.add("lambda");
        if (expr.matches("(?s).*\\bd\\b.*")) out.add("d");
    }

    private String sanitizeFactorizeVar(String v) {
        if (v == null) return "d";
        return switch (v.toLowerCase()) {
            case "x"                  -> "x";
            case "l"                  -> "l";
            case "lam", "lambda", "λ" -> "lam";
            default                   -> "d";
        };
    }

    private Map<String, Double> buildVariableMap(
            DeterminantRequest req,
            Set<String> varsUsed,
            List<String> xLVars,
            List<String> tLVars) {

        double x      = req.getXValue()      != null ? req.getXValue()      : 0.0;
        double l      = req.getLValue()       != null ? req.getLValue()       : 0.0;
        double d      = req.getDValue()       != null ? req.getDValue()       : 0.0;
        double lambda = req.getLambdaValue()  != null ? req.getLambdaValue()  : 0.0;

        Map<String, Double> vars = new HashMap<>();
        vars.put("x",      x);
        vars.put("l",      l);
        vars.put("d",      d);
        vars.put("lambda", lambda);

        for (String varName : xLVars) {
            int offset    = ComputeOffset.getOffset(varName);
            double lShift = l + offset;
            vars.put(varName, x - 2.0 * (lShift - 1.0) * lShift);
        }

        for (String varName : tLVars) {
            int offset    = ComputeOffset.getOffset(varName);
            double lShift = l + offset;
            double xL     = x - 2.0 * (lShift - 1.0) * lShift;
            double xLp1   = x - 2.0 * lShift * (lShift + 1.0);
            vars.put(varName, xL * xLp1 - d);
        }

        varsUsed.forEach(v -> vars.putIfAbsent(v, 0.0));
        return vars;
    }
}