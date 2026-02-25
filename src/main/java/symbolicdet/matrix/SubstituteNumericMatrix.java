package symbolicdet.matrix;

import java.math.BigDecimal;
import java.util.Map;

public class SubstituteNumericMatrix {
    public static BigDecimal[][] substituteNumericMatrix(Matrix matrix, Map<String, Double> variables) {
        BigDecimal[][] numeric = new BigDecimal[matrix.getN()][matrix.getN()];

        for (int i = 0; i < matrix.getN(); i++) {
            for (int j = 0; j < matrix.getN(); j++) {
                numeric[i][j] = matrix.getData()[i][j].evalBD(variables);
            }
        }
        return numeric;
    }
}
