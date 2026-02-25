package symbolicdet.utils;

import symbolicdet.matrix.Matrix;
import symbolicdet.symbolic.expressions.Variable;

import java.util.HashSet;
import java.util.Set;

public class ExtractVariablesFromMatrix {
    public static Set<String> collectVars(Matrix matrix) {
        Set<String> variables = new HashSet<>();
        for (int i = 0; i < matrix.getN(); i++)
            for (int j = 0; j < matrix.getN(); j++)
                if (matrix.getData()[i][j] instanceof Variable v) variables.add(v.getName());
        return variables;
    }
}
