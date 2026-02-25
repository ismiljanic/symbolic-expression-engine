package symbolicdet.utils;

import symbolicdet.cli.NumericInputs;

import java.util.List;
import java.util.Map;

import static symbolicdet.utils.ComputeOffset.getOffset;

public class PopulateTLVariables {
    // --- 5. Populate t_l variables (t_l = x_l * x_{l+1} - D) ---
    public static void populateTLVars(Map<String, Double> variables, List<String> tLVars, NumericInputs inputs) {
        for (String value : tLVars) {
            int offset = getOffset(value); // e.g., -2, -1, +1, 0
            double lShifted = inputs.getlValue() + offset;

            // compute x_{l+n} and x_{l+n+1}
            double x_l_val = inputs.getxValue() - 2 * (lShifted - 1) * lShifted;
            double x_l_plus1_val = inputs.getxValue() - 2 * (lShifted + 1 - 1) * (lShifted + 1);
            double t_val = (x_l_val * x_l_plus1_val) - inputs.getdValue();
            variables.put(value, t_val);

            System.out.printf("Computed %s = (%f * %f) - %f = %f%n", value, x_l_val, x_l_plus1_val, inputs.getdValue(), t_val);
        }
    }

    public static void populateTLVar(Map<String, Double> variables, String value, NumericInputs inputs) {
        int offset = getOffset(value);
        double lShifted = inputs.getlValue() + offset;
        double x_l_val = inputs.getxValue() - 2 * (lShifted - 1) * lShifted;
        double x_l_plus1_val = inputs.getxValue() - 2 * (lShifted + 1 - 1) * (lShifted + 1);
        variables.put(value, x_l_val * x_l_plus1_val - inputs.getdValue());
    }
}
