package symbolicdet.utils;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class PopulateRemainingVariables {
    // --- 6. Handle user-defined variables (not x_l / t_l) ---
    public static void populateRemainingVars(Scanner scanner, Map<String, Double> variables, Set<String> varsUsed) {
        for (String value : varsUsed) {
            if (variables.containsKey(value)) continue;

            boolean isNegative = value.startsWith("-");
            String base = isNegative ? value.substring(1) : value;

            System.out.print("Value for " + base + ": ");
            double val = scanner.nextDouble();
            variables.put(value, isNegative ? -val : val);
        }
    }
}
