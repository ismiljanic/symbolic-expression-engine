package symbolicdet.utils;

import java.math.BigDecimal;
import java.math.MathContext;

public class TraceNumericDeterminant {
    public static BigDecimal tracedNumericDeterminant(BigDecimal[][] matrix, SymbolicTracer tracer, boolean detailedOutput) {
        int matrixLength = matrix.length;
        BigDecimal det = BigDecimal.ONE;
        MathContext mc = MathContext.DECIMAL128;

        tracer.log("Starting LU-based determinant calculation for " + matrixLength + "x" + matrixLength + " matrix.");
        tracer.log("Initial matrix:");
        PrintNumericMatrix.printNumericMatrix(matrix, tracer);

        for (int i = 0; i < matrixLength; i++) {
            // Find pivot
            int pivot = i;
            BigDecimal maxPivot = matrix[i][i].abs();
            for (int r = i + 1; r < matrixLength; r++) {
                BigDecimal absVal = matrix[r][i].abs();
                if (absVal.compareTo(maxPivot) > 0) {
                    pivot = r;
                    maxPivot = absVal;
                }
            }
            if (detailedOutput)
                tracer.log("Selecting pivot for column " + i + ": row " + pivot + " (value = " + matrix[pivot][i] + ")");

            if (matrix[pivot][i].compareTo(BigDecimal.ZERO) == 0) {
                tracer.log("Pivot is zero. Determinant is 0.");
                return BigDecimal.ZERO;
            }

            // Swap rows if necessary
            if (i != pivot) {
                BigDecimal[] tmp = matrix[i];
                matrix[i] = matrix[pivot];
                matrix[pivot] = tmp;
                det = det.negate();
                if (detailedOutput) {
                    tracer.log("Swapped rows " + i + " and " + pivot + ". Updated det sign.");
                    tracer.log("Matrix after row swap:");
                    PrintNumericMatrix.printNumericMatrix(matrix, tracer);
                }
            }

            det = det.multiply(matrix[i][i], mc);
            if (detailedOutput)
                tracer.log("Pivot [" + i + "," + i + "] = " + matrix[i][i] + ". Partial determinant = " + det);

            for (int r = i + 1; r < matrixLength; r++) {
                BigDecimal factor = matrix[r][i].divide(matrix[i][i], mc);
                for (int c = i; c < matrixLength; c++) {
                    matrix[r][c] = matrix[r][c].subtract(factor.multiply(matrix[i][c], mc), mc);
                    if (detailedOutput)
                        tracer.log("Eliminating row " + r + " using factor (" + matrix[r][c] + " / " + matrix[c][c] + ") = " + factor + " * row " + c);
                }
            }

            if (detailedOutput) {
                tracer.log("Matrix after eliminating column " + i + ":");
                PrintNumericMatrix.printNumericMatrix(matrix, tracer);
            }
        }

        if (detailedOutput) tracer.log("Finished LU elimination. Determinant = " + det);
        return det;
    }
}
