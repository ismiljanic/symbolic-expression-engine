package symbolicdet.utils;

import symbolicdet.cli.NumericInputs;

import java.util.Map;

import static symbolicdet.utils.ComputeOffset.getOffset;

public class PopulateXLVariables {
    public static void populateXLVar(Map<String, Double> variables, String value, NumericInputs inputs) {
        int offset = getOffset(value);
        double lShifted = inputs.getlValue() + offset;
        double x_l_val = inputs.getxValue() - 2 * (lShifted - 1) * lShifted;
        variables.put(value, x_l_val);
    }
}
