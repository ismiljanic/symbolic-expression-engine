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

        String[][] raw = req.getMatrix();
        int n = raw.length;

        Set<String> varsUsed = new LinkedHashSet<>();
        Symbol[][] data = new Symbol[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (raw[i] == null || j >= raw[i].length || raw[i][j] == null || raw[i][j].isBlank()) {
                    return DeterminantResponse.error(
                            "Missing matrix element at [" + (i + 1) + "," + (j + 1) + "]");
                }
                data[i][j] = parseSymbolicInput(raw[i][j].trim(), varsUsed);
            }
        }

        Matrix matrix = new Matrix(data);

        // ===============================
        // FAST PATH — PURE NUMERIC INPUT
        // ===============================
        if (isPureNumeric(matrix)) {
            return computePureNumeric(matrix, req);
        }

        // ===============================
        // SYMBOLIC PATH
        // ===============================
        List<String> xLVars = varsUsed.stream()
                .filter(v -> v.matches("x_l([+-]\\d+)?"))
                .toList();

        List<String> tLVars = varsUsed.stream()
                .filter(v -> v.matches("t_l([+-]\\d+)?"))
                .toList();

        boolean hasLambda = varsUsed.stream()
                .anyMatch(v -> v.equals("lambda") || v.matches("lambda([+-]\\d+)?"));

        boolean hasD = varsUsed.contains("d");

        DeterminantResponse response = new DeterminantResponse();
        response.setDetectedVariables(new ArrayList<>(varsUsed));
        response.setNeedsXL(!xLVars.isEmpty() || !tLVars.isEmpty());
        response.setNeedsD(hasD || !tLVars.isEmpty());
        response.setNeedsLambda(hasLambda);

        SymbolicTracer tracer = new SymbolicTracer(req.isDetailedLU());
        Symbol det = matrix.determinant(tracer, null);
        String expr = det.toExpr();
        response.setSymbolicDeterminant(expr);

        String expanded = ExpandSymbolic.expandSymbolic(expr);
        String simplified = simplifySymbolic(expanded);
        String expandedPoly = PolySimplifier.expandSymbolicOnly(simplified);
        response.setExpandedPolynomial(expandedPoly);

        if (req.isDetailedLU()) {
            response.setLuSteps(tracer.getLog());
        }

        if (req.isFactorize()) {
            List<FactorizedGroup> factorized =
                    FactorizeFinalExpression.factor(expandedPoly, n);
            response.setFactorized(factorized);
        }

        if (req.hasNumericInputs()) {
            try {
                Map<String, Double> variables =
                        buildVariableMap(req, varsUsed, xLVars, tLVars);

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

    // ===============================
    // NUMERIC-ONLY IMPLEMENTATION
    // ===============================
    private DeterminantResponse computePureNumeric(Matrix matrix, DeterminantRequest req) {

        int n = matrix.getN();
        BigDecimal[][] numericData = new BigDecimal[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                numericData[i][j] =
                        BigDecimal.valueOf(matrix.getData()[i][j].eval(Collections.emptyMap()));
            }
        }

        SymbolicTracer tracer = new SymbolicTracer(req.isDetailedLU());
        BigDecimal det =
                tracedNumericDeterminant(numericData, tracer, req.isDetailedLU());

        return getDeterminantResponse(req, det, tracer);
    }

    /* Pure numeric */
    private static DeterminantResponse getDeterminantResponse(DeterminantRequest req, BigDecimal det, SymbolicTracer tracer) {
        DeterminantResponse r = new DeterminantResponse();

        r.setNumericFromLU(det.doubleValue());
        r.setDetectedVariables(Collections.emptyList());
        r.setNeedsXL(false);
        r.setNeedsD(false);
        r.setNeedsLambda(false);

        r.setSymbolicDeterminant(null);
        r.setExpandedPolynomial(null);
        r.setFactorized(Collections.emptyList());

        if (req.isDetailedLU()) {
            r.setLuSteps(tracer.getLog());
        }
        return r;
    }

    private boolean isPureNumeric(Matrix matrix) {
        for (int i = 0; i < matrix.getN(); i++) {
            for (int j = 0; j < matrix.getN(); j++) {
                if (!CheckIfNumeric.isNumeric(matrix.getData()[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===============================
    // VARIABLE MAP
    // ===============================
    private Map<String, Double> buildVariableMap(
            DeterminantRequest req,
            Set<String> varsUsed,
            List<String> xLVars,
            List<String> tLVars) {

        double x = req.getXValue();
        double l = req.getLValue();
        double d = req.getDValue() != null ? req.getDValue() : 0.0;
        double lambda = req.getLambdaValue() != null ? req.getLambdaValue() : 0.0;

        Map<String, Double> vars = new HashMap<>();

        vars.put("x", x);
        vars.put("l", l);
        vars.put("d", d);
        vars.put("lambda", lambda);

        for (String varName : xLVars) {
            int offset = ComputeOffset.getOffset(varName);
            double lShifted = l + offset;
            double xVal = x - 2.0 * (lShifted - 1.0) * lShifted;
            vars.put(varName, xVal);
        }

        for (String varName : tLVars) {
            int offset = ComputeOffset.getOffset(varName);
            double lShifted = l + offset;
            double xL = x - 2.0 * (lShifted - 1.0) * lShifted;
            double xLp1 = x - 2.0 * lShifted * (lShifted + 1.0);
            vars.put(varName, xL * xLp1 - d);
        }

        for (String varName : varsUsed) {
            vars.putIfAbsent(varName, 0.0);
        }

        return vars;
    }
}