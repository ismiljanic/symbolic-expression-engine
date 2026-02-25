package symbolicdet.symbolic.eval;

import symbolicdet.matrix.Matrix;
import symbolicdet.symbolic.expressions.Constant;
import symbolicdet.symbolic.expressions.Expression;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.utils.SymbolicTracer;

import static symbolicdet.utils.PrintSymbolicMatrix.printSymbolicMatrix;

public class CalculateSymbolicLUDeterminant {
    public static Symbol symbolicLUDeterminant(Matrix m, SymbolicTracer tracer, boolean detailedOutput) {
        int n = m.getN();
        Symbol[][] mat = new Symbol[n][n];

        // Copy matrix data to work on
        for (int i = 0; i < n; i++)
            System.arraycopy(m.getData()[i], 0, mat[i], 0, n);

        Symbol detSign = new Constant(1);
        Symbol det = new Constant(1);

        if (detailedOutput)
            tracer.log("Starting symbolic LU-based determinant calculation for " + n + "x" + n + " matrix.");

        for (int i = 0; i < n; i++) {
            int pivot = i;
            while (pivot < n && mat[pivot][i].toExpr().equals("0")) pivot++;

            if (pivot == n) {
                if (detailedOutput) tracer.log("Entire column " + i + " is zero. Determinant = 0.");
                return new Constant(0);
            }

            if (pivot != i) {
                Symbol[] tmp = mat[i];
                mat[i] = mat[pivot];
                mat[pivot] = tmp;
                detSign = new Expression(detSign, '*', new Constant(-1));
                if (detailedOutput) {
                    tracer.log("Swapped rows " + i + " and " + pivot + ". Updated sign to " + detSign.toExpr());
                    printSymbolicMatrix(mat, tracer);
                }
            }

            if (detailedOutput) tracer.log("Pivot element at [" + i + "," + i + "] = " + mat[i][i].toExpr());

            det = new Expression(det, '*', mat[i][i]);
            if (detailedOutput) tracer.log("Partial determinant after multiplying by pivot: " + det.toExpr());

            for (int j = i + 1; j < n; j++) {
                Symbol factor = new Expression(mat[j][i], '/', mat[i][i]);
                for (int k = i; k < n; k++) {
                    mat[j][k] = new Expression(mat[j][k], '-', new Expression(factor, '*', mat[i][k]));
                }
                if (detailedOutput) {
                    tracer.log("Eliminated row " + j + " using factor " + factor.toExpr());
                    printSymbolicMatrix(mat, tracer);
                }
            }
        }

        det = new Expression(detSign, '*', det);
        if (detailedOutput) tracer.log("Final symbolic determinant: " + det.toExpr());

        return det;
    }
}
