package symbolicdet.utils;

import java.math.BigDecimal;

public class PrintNumericMatrix {
    public static void printNumericMatrix(BigDecimal[][] matrix, SymbolicTracer tracer) {
        for (BigDecimal[] row : matrix) {
            StringBuilder stringBuilder = new StringBuilder();
            for (BigDecimal value : row) stringBuilder.append(String.format("%10.4f", value)).append(" ");
            tracer.log(stringBuilder.toString());
        }
        tracer.log("");
    }
}
