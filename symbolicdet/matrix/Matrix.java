package symbolicdet.matrix;

import symbolicdet.symbolic.expressions.Constant;
import symbolicdet.symbolic.expressions.Expression;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.expressions.Variable;
import symbolicdet.utils.SymbolicTracer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Matrix {
    private Symbol[][] data;
    private int n;
    private String name;

    public Matrix(Symbol[][] data) {
        this(data, "M");
    }

    public Matrix(Symbol[][] data, String name) {
        this.data = data;
        this.n = data.length;
        this.name = name;
    }

    // Symbolic determinant for small matrices
    public Symbol determinant(SymbolicTracer tracer, Map<String, Double> numericVars) {
        if (n == 1) return data[0][0];
        if (n == 2) {
            Symbol minorDeterminant = new Expression(new Expression(data[0][0], '*', data[1][1]), '-', new Expression(data[0][1], '*', data[1][0]));
            if (numericVars != null) tracer.log("Numeric 2x2 determinant: " + minorDeterminant.eval(numericVars));
            return minorDeterminant;
        }

        AtomicReference<Symbol> determinant = new AtomicReference<>(new Constant(0));
        for (int c = 0; c < n; c++) {
            Matrix minor = minor(0, c);
            Symbol element = data[0][c];
            Symbol sign = (c % 2 == 0) ? new Constant(1) : new Constant(-1);

            tracer.log("Using element [0," + c + "] = " + element.toExpr());
            tracer.log("Minor matrix:");
            minor.printMatrix();

            Symbol minorDeterminant = minor.determinant(tracer, numericVars);
            Symbol term = simplify(multiplyIfNotZero(sign, multiplyIfNotZero(element, minorDeterminant)));

            if (!isZero(term)) {
                if (numericVars != null) {
                    tracer.log("Numeric value of this term: " + term.eval(numericVars));
                }
                tracer.log("Symbolic term to add: " + term.toExpr() + "\n");
                determinant.set(new Expression(determinant.get(), '+', term));
            } else {
                tracer.log("Skipping zero term: " + term.toExpr());
            }
        }
        return determinant.get();
    }

    public static Symbol simplify(Symbol symbol) {
        if (symbol instanceof Constant || symbol instanceof Variable || symbol == null) {
            return symbol; // nothing to simplify
        }

        if (symbol instanceof Expression expression) {
            expression.setLeft(simplify(expression.getLeft()));
            expression.setRight(simplify(expression.getRight()));

            switch (expression.getOperator()) {
                case '*':
                    if (isZero(expression.getLeft()) || isZero(expression.getRight())) return new Constant(0);
                    if (isOne(expression.getLeft())) return expression.getRight();
                    if (isOne(expression.getRight())) return expression.getLeft();
                    break;
                case '+':
                    if (isZero(expression.getLeft())) return expression.getRight();
                    if (isZero(expression.getRight())) return expression.getLeft();
                    break;
                case '-':
                    if (isZero(expression.getLeft()))
                        return new Expression(new Constant(0), '-', expression.getRight());
                    if (isZero(expression.getRight())) return expression.getLeft();
                    break;
                case '/':
                    if (isZero(expression.getLeft())) return new Constant(0);
                    if (isOne(expression.getRight())) return expression.getLeft();
                    break;
            }
            return expression;
        }
        // fallback for unexpected types
        return symbol;
    }

    public static boolean isOne(Symbol symbol) {
        return symbol instanceof Constant constant && Math.abs(constant.getValue() - 1.0) < 1e-12;
    }

    public static Symbol multiplyIfNotZero(Symbol leftSymbol, Symbol rightSymbol) {
        if (isZero(leftSymbol) || isZero(rightSymbol)) return new Constant(0);
        return new Expression(leftSymbol, '*', rightSymbol);
    }

    public static boolean isZero(Symbol symbol) {
        if (symbol instanceof Constant constant) {
            return Math.abs(constant.getValue()) < 1e-12;
        } else if (symbol instanceof Expression expression) {
            if (expression.getOperator() == '*') {
                return isZero(expression.getLeft()) || isZero(expression.getRight());
            } else if (expression.getOperator() == '+' || expression.getOperator() == '-') {
                return isZero(expression.getLeft()) && isZero(expression.getRight());
            } else if (expression.getOperator() == '/') {
                return isZero(expression.getLeft()); // numerator zero → zero
            }
        }
        return false;
    }

    public void printMatrix() {
        int columnWidth = 30;

        System.out.print("      ");
        for (int j = 0; j < n; j++) {
            System.out.printf("%-" + columnWidth + "s", "Col " + j);
        }
        System.out.println();

        System.out.print("      ");
        for (int j = 0; j < n; j++) System.out.print("-".repeat(columnWidth));
        System.out.println();

        for (int i = 0; i < n; i++) {
            System.out.printf("Row %-3d", i);
            for (int j = 0; j < n; j++) {
                String expr = data[i][j].toExpr();
                if (expr.length() > columnWidth - 1) expr = expr.substring(0, columnWidth - 4) + "...";
                System.out.printf("%-" + columnWidth + "s", expr);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Returns the mathematical minor of this matrix by removing the specified row and column.
     */
    public Matrix minor(int rowToRemove, int colToRemove) {
        Symbol[][] minorData = new Symbol[n - 1][n - 1];
        int newRowIndex = 0;

        for (int i = 0; i < n; i++) {
            if (i == rowToRemove) continue;

            int newColIndex = 0;
            for (int j = 0; j < n; j++) {
                if (j == colToRemove) continue;

                minorData[newRowIndex][newColIndex++] = data[i][j];
            }
            newRowIndex++;
        }

        return new Matrix(minorData, name + "_minor" + rowToRemove + "_" + colToRemove);
    }

    /**
     * Returns a new matrix by excluding the specified row and column.
     * Essentially the same as minor(), but named for general-purpose matrix reduction.
     */
    public Matrix subMatrixExcluding(int rowToRemove, int colToRemove) {
        Symbol[][] reducedData = new Symbol[n - 1][n - 1];
        int newRowIndex = 0;

        for (int i = 0; i < n; i++) {
            if (i == rowToRemove) continue;

            int newColIndex = 0;
            for (int j = 0; j < n; j++) {
                if (j == colToRemove) continue;

                reducedData[newRowIndex][newColIndex++] = data[i][j];
            }
            newRowIndex++;
        }

        return new Matrix(reducedData, name + "_reduced" + rowToRemove + "_" + colToRemove);
    }

    public Symbol[][] getData() {
        return data;
    }

    public void setData(Symbol[][] data) {
        this.data = data;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}