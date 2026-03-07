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
            Symbol minorDeterminant = new Expression(
                    new Expression(data[0][0], '*', data[1][1]),
                    '-',
                    new Expression(data[0][1], '*', data[1][0])
            );
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
            return symbol;
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
                return isZero(expression.getLeft());
            }
        }
        return false;
    }

    public void printMatrixFull() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                String expr = data[i][j].toExpr();
                System.out.println("row[" + i + "," + j + "] = " + expr);
            }
        }
        System.out.println();
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

    public boolean isTridiagonal() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (Math.abs(i - j) > 1) {
                    if (!data[i][j].toExpr().equals("0")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Symbolic x_{l + offset}.
     * Variable name format: "x_l+3", "x_l-2", "x_l+0"
     * NO parentheses — must match regex x_l([+-]\d+)? used in DeterminantService and ComputeOffset.
     */
    private static Symbol x_l_formula(int offset) {
        // match the regex x_l([+-]\d+)? and are handled by populateXLVar / getOffset.
        String offsetStr = (offset >= 0) ? "+" + offset : String.valueOf(offset);
        return new Variable("x_l" + offsetStr);
    }

    /**
     * Builds main diagonal element A_k symbolically.
     */
    private static Symbol buildA_k(int n, int k, Variable lambda) {
        Symbol term1 = x_l_formula(-n + 1);
        Symbol term2 = x_l_formula(2 - k);

        int coeff1 = n * (n + 1) / 2;
        int coeff2 = (n * (n - 1) / 2) + ((n + 1 - k) * (n + 1 - k) - n * n);

        Symbol t1 = new Expression(new Constant(coeff1), '*', term1);
        Symbol t2 = new Expression(new Constant(coeff2), '*', term2);

        return new Expression(new Expression(t1, '+', t2), '-', lambda);
    }

    private static Symbol buildB_k(int n, int k, Variable D) {
        int coeff = -((n - k + 1) * (n - k) / 2);

        Symbol x1 = x_l_formula(1 - k);
        Symbol x2 = x_l_formula(2 - k);

        Symbol inside = new Expression(new Expression(x1, '*', x2), '-', D);
        return new Expression(new Constant(coeff), '*', inside);
    }

    /**
     * Builds lower diagonal element C_k symbolically.
     */
    private static Symbol buildC_k(int n, int k) {
        return new Constant(n * (n + 1) / 2.0 - ((n + 1 - k) * (n + 2 - k) / 2.0));
    }

    /**
     * Builds the full tridiagonal matrix symbolically.
     */
    public static Matrix buildSpecialMatrix(int n) {
        Symbol[][] data = new Symbol[n][n];
        Variable lambda = new Variable("lambda");
        Variable D = new Variable("d");

        for (int i = 0; i < n; i++) {
            int k = i + 1;

            data[i][i] = buildA_k(n, k, lambda);
            if (i < n - 1) data[i][i + 1] = buildB_k(n, k, D);
            if (i > 0) data[i][i - 1] = buildC_k(n, k);

            for (int j = 0; j < n; j++) {
                if (j != i && j != i + 1 && j != i - 1) data[i][j] = new Constant(0);
            }
        }

        return new Matrix(data);
    }

    /**
     * Computes the determinant of a tridiagonal matrix using the standard recurrence relation.
     * D0 = 1, D1 = A1, Dk = Ak * D_{k-1} - Ck * B_{k-1} * D_{k-2}
     */
    public Symbol determinantTridiagonal() {
        if (n == 0) return new Constant(1);
        if (n == 1) return data[0][0];

        Symbol D_prev2 = new Constant(1);
        Symbol D_prev1 = data[0][0];

        for (int k = 1; k < n; k++) {
            Symbol A_k = data[k][k];
            Symbol C_k = data[k][k - 1];
            Symbol B_k_minus_1 = data[k - 1][k];

            Symbol term1 = new Expression(A_k, '*', D_prev1);
            Symbol cbProduct = new Expression(C_k, '*', B_k_minus_1);
            Symbol term2 = new Expression(cbProduct, '*', D_prev2);
            Symbol D_current = new Expression(term1, '-', term2);

            D_prev2 = D_prev1;
            D_prev1 = D_current;
        }

        return D_prev1;
    }

    public Symbol determinantSmart(SymbolicTracer tracer) {
        if (isTridiagonal()) {
            System.out.println("Using O(n) tridiagonal determinant.");
            return determinantTridiagonal();
        }
        return determinant(tracer, null);
    }

    public Symbol[][] getData() { return data; }
    public void setData(Symbol[][] data) { this.data = data; }
    public int getN() { return n; }
    public void setN(int n) { this.n = n; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}