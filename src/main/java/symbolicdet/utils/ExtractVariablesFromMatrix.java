package symbolicdet.utils;

import symbolicdet.matrix.Matrix;
import symbolicdet.symbolic.expressions.Expression;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.expressions.Variable;

import java.util.HashSet;
import java.util.Set;

public class ExtractVariablesFromMatrix {

    /**
     * Collects all variable names from the matrix by deeply walking every cell's
     * expression tree. Previously this only caught top-level Variable nodes,
     * missing variables nested inside Expression trees (e.g. x_l+0 inside A_k).
     */
    public static Set<String> collectVars(Matrix matrix) {
        Set<String> variables = new HashSet<>();
        for (int i = 0; i < matrix.getN(); i++)
            for (int j = 0; j < matrix.getN(); j++)
                collectFromSymbol(matrix.getData()[i][j], variables);
        return variables;
    }

    /**
     * Recursively walks a Symbol tree and adds all Variable names found.
     */
    public static void collectFromSymbol(Symbol s, Set<String> result) {
        if (s instanceof Variable v) {
            result.add(v.getName());
        } else if (s instanceof Expression e) {
            collectFromSymbol(e.getLeft(), result);
            collectFromSymbol(e.getRight(), result);
        }
        // Constant: nothing to add
    }

    /**
     * Collects all variable names reachable from a single Symbol tree.
     * Useful for scanning the determinant expression tree before evalBD.
     */
    public static Set<String> collectFromSymbolTree(Symbol s) {
        Set<String> result = new HashSet<>();
        collectFromSymbol(s, result);
        return result;
    }
}