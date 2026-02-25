package symbolicdet.utils;

import symbolicdet.symbolic.expressions.Symbol;

public class PrintSymbolicMatrix {
    public static void printSymbolicMatrix(Symbol[][] matrix, SymbolicTracer tracer) {
        tracer.log("Current matrix state:");
        for (Symbol[] row : matrix) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Symbol symbol : row) stringBuilder.append(symbol.toExpr()).append("\t");
            tracer.log(stringBuilder.toString());
        }
        tracer.log("");
    }
}
