import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    // === Abstract Symbol ===
    static abstract class Symbol {
        abstract String toExpr();

        abstract double eval(Map<String, Double> vars);
    }

    // === Variable ===
    static class Variable extends Symbol {
        String name;

        Variable(String name) {
            this.name = name;
        }

        @Override
        public String toExpr() {
            return name;
        }

        @Override
        public double eval(Map<String, Double> vars) {
            if (!vars.containsKey(name)) throw new RuntimeException("Missing value for variable: " + name);
            return vars.get(name);
        }
    }

    // === Constant ===
    static class Constant extends Symbol {
        double value;

        Constant(double value) {
            this.value = value;
        }

        @Override
        public String toExpr() {
            return Double.toString(value);
        }

        @Override
        public double eval(Map<String, Double> vars) {
            return value;
        }
    }

    // === Expression ===
    static class Expression extends Symbol {
        Symbol left, right;
        char op;

        Expression(Symbol left, char op, Symbol right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public String toExpr() {
            return "(" + left.toExpr() + " " + op + " " + right.toExpr() + ")";
        }

        @Override
        public double eval(Map<String, Double> vars) {
            double a = left.eval(vars), b = right.eval(vars);
            return switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }
    }

    // === Symbolic tracer ===
    static class SymbolicTracer {
        private int depth = 0;

        void log(String msg) {
            System.out.println("  ".repeat(depth) + msg);
        }

        void indent() {
            depth++;
        }

        void unindent() {
            if (depth > 0) depth--;
        }
    }

    // === Matrix class ===
    static class Matrix {
        Symbol[][] data;
        int n;
        String name;

        Matrix(Symbol[][] data) {
            this(data, "M");
        }

        Matrix(Symbol[][] data, String name) {
            this.data = data;
            this.n = data.length;
            this.name = name;
        }

        // Symbolic determinant for small matrices
        Symbol determinant(SymbolicTracer tracer, Map<String, Double> numericVars) {
            if (n == 1) return data[0][0];
            if (n == 2) {
                Symbol minorDet = new Expression(
                        new Expression(data[0][0], '*', data[1][1]),
                        '-', new Expression(data[0][1], '*', data[1][0])
                );
                if (numericVars != null) tracer.log("Numeric 2x2 determinant: " + minorDet.eval(numericVars));
                return minorDet;
            }

            AtomicReference<Symbol> det = new AtomicReference<>(new Constant(0));
            for (int c = 0; c < n; c++) {
                Matrix minor = minor(0, c);
                Symbol elem = data[0][c];
                Symbol sign = (c % 2 == 0) ? new Constant(1) : new Constant(-1);

                tracer.log("Using element [0," + c + "] = " + elem.toExpr());
                tracer.log("Minor matrix:");
                minor.printMatrix();

                Symbol minorDet = minor.determinant(tracer, numericVars);
                Symbol term = new Expression(sign, '*', new Expression(elem, '*', minorDet));

                if (numericVars != null) tracer.log("Numeric value of this term: " + term.eval(numericVars));
                tracer.log("Symbolic term to add: " + term.toExpr() + "\n");

                det.set(new Expression(det.get(), '+', term));
            }
            return det.get();
        }

        void printMatrix() {
            System.out.println("Matrix " + name + ":");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) System.out.print(data[i][j].toExpr() + "\t");
                System.out.println();
            }
            System.out.println();
        }

        Matrix minor(int row, int col) {
            Symbol[][] m = new Symbol[n - 1][n - 1];
            int mi = 0;
            for (int i = 0; i < n; i++) {
                if (i == row) continue;
                int mj = 0;
                for (int j = 0; j < n; j++) {
                    if (j == col) continue;
                    m[mi][mj++] = data[i][j];
                }
                mi++;
            }
            return new Matrix(m, name + "_m" + row + col);
        }
    }

    // === Numeric LU determinant with step-by-step trace ===
    static double tracedNumericDeterminant(double[][] mat, SymbolicTracer tracer, boolean detailedOutput) {
        int n = mat.length;
        double det = 1.0;

        tracer.log("Starting LU-based determinant calculation for " + n + "x" + n + " matrix.");
        tracer.log("Initial matrix:");
        printNumericMatrix(mat, tracer);

        for (int i = 0; i < n; i++) {
            // Find pivot
            int pivot = i;
            double maxPivot = Math.abs(mat[i][i]);
            for (int j = i + 1; j < n; j++) {
                double absVal = Math.abs(mat[j][i]);
                if (absVal > maxPivot) {
                    pivot = j;
                    maxPivot = absVal;
                }
            }
            if (detailedOutput)
                tracer.log("Selecting pivot for column " + i + ": row " + pivot + " (value = " + mat[pivot][i] + ")");

            if (Math.abs(mat[pivot][i]) < 1e-10) {
                if (detailedOutput) tracer.log("Pivot is too small. Determinant is 0.");
                return 0.0;
            }

            // Swap rows if necessary
            if (i != pivot) {
                double[] tmp = mat[i];
                mat[i] = mat[pivot];
                mat[pivot] = tmp;
                det *= -1;
                if (detailedOutput) {
                    tracer.log("Swapped rows " + i + " and " + pivot + ". Updated det sign.");
                    tracer.log("Matrix after row swap:");
                    printNumericMatrix(mat, tracer);
                }
            }

            // Multiply determinant by pivot element
            det *= mat[i][i];
            if (detailedOutput)
                tracer.log("Pivot [" + i + "," + i + "] = " + mat[i][i] + ". Partial determinant = " + det);

            // Eliminate below pivot
            for (int j = i + 1; j < n; j++) {
                double factor = mat[j][i] / mat[i][i];
                for (int k = i; k < n; k++) mat[j][k] -= factor * mat[i][k];
                if (detailedOutput)
                    tracer.log("Eliminating row " + j + " using factor " + factor + " * row " + i);
            }

            if (detailedOutput) {
                tracer.log("Matrix after eliminating column " + i + ":");
                printNumericMatrix(mat, tracer);
            }
        }

        if (detailedOutput) tracer.log("Finished LU elimination. Determinant = " + det);
        return det;
    }

    // Helper method to nicely print numeric matrices for tracer
    static void printNumericMatrix(double[][] mat, SymbolicTracer tracer) {
        for (double[] row : mat) {
            StringBuilder sb = new StringBuilder();
            for (double val : row) sb.append(String.format("%10.4f", val)).append(" ");
            tracer.log(sb.toString());
        }
        tracer.log("");
    }

    // --- Symbolic LU determinant for any size matrix ---
    static Symbol symbolicLUDeterminant(Matrix m, SymbolicTracer tracer, boolean detailedOutput) {
        int n = m.n;
        Symbol[][] mat = new Symbol[n][n];

        // Copy matrix data to work on
        for (int i = 0; i < n; i++)
            System.arraycopy(m.data[i], 0, mat[i], 0, n);

        Symbol detSign = new Constant(1);
        Symbol det = new Constant(1);

        if (detailedOutput) tracer.log("Starting symbolic LU-based determinant calculation for " + n + "x" + n + " matrix.");

        for (int i = 0; i < n; i++) {
            // --- Find pivot: first non-zero element in column i ---
            int pivot = i;
            while (pivot < n && mat[pivot][i].toExpr().equals("0")) pivot++;

            if (pivot == n) {
                if (detailedOutput) tracer.log("Entire column " + i + " is zero. Determinant = 0.");
                return new Constant(0);
            }

            if (pivot != i) {
                // Swap rows
                Symbol[] tmp = mat[i];
                mat[i] = mat[pivot];
                mat[pivot] = tmp;
                detSign = new Expression(detSign, '*', new Constant(-1));
                if (detailedOutput) {
                    tracer.log("Swapped rows " + i + " and " + pivot + ". Updated sign to " + detSign.toExpr());
                    printSymbolicMatrix(mat, tracer);
                }
            }

            if (detailedOutput)
                tracer.log("Pivot element at [" + i + "," + i + "] = " + mat[i][i].toExpr());

            // Multiply determinant by pivot element
            det = new Expression(det, '*', mat[i][i]);
            if (detailedOutput) tracer.log("Partial determinant after multiplying by pivot: " + det.toExpr());

            // --- Eliminate below pivot ---
            for (int j = i + 1; j < n; j++) {
                Symbol factor = new Expression(mat[j][i], '/', mat[i][i]);
                for (int k = i; k < n; k++) {
                    mat[j][k] = new Expression(mat[j][k], '-', new Expression(factor, '*', mat[i][k]));
                }
                if (detailedOutput) {
                    tracer.log("Eliminated row " + j + " using factor " + factor.toExpr());
                    printSymbolicMatrix(mat, tracer);
                }
            }
        }

        // Multiply by row swap sign
        det = new Expression(detSign, '*', det);
        if (detailedOutput) tracer.log("Final symbolic determinant: " + det.toExpr());

        return det;
    }

    // Helper to print symbolic matrix nicely
    private static void printSymbolicMatrix(Symbol[][] mat, SymbolicTracer tracer) {
        tracer.log("Current matrix state:");
        for (Symbol[] row : mat) {
            StringBuilder sb = new StringBuilder();
            for (Symbol s : row) sb.append(s.toExpr()).append("\t");
            tracer.log(sb.toString());
        }
        tracer.log("");
    }

    // Helper method to read yes/no input
    private static boolean askYesNo(Scanner sc, String prompt) {
        String input;
        while (true) {
            System.out.print(prompt + " (yes/no): ");
            input = sc.nextLine().trim().toLowerCase();
            if (input.equals("yes")) return true;
            if (input.equals("no")) return false;
            System.out.println("Invalid input. Please type 'yes' or 'no'.");
        }
    }


    public static void main(String[] args) {
        final int SYMBOLIC_LIMIT = 3; // recursive symbolic determinant limit
        Scanner sc = new Scanner(System.in);
        SymbolicTracer tracer = new SymbolicTracer();

        System.out.print("Enter matrix size (n): ");
        int n = sc.nextInt();
        sc.nextLine();

        Symbol[][] matrix = new Symbol[n][n];
        Set<String> varsUsed = new HashSet<>();

        // Input or generate matrix
        if (n > SYMBOLIC_LIMIT) {
            boolean manual = askYesNo(sc, "Do you want to enter matrix manually?");

            if (manual) {
                System.out.println("Enter matrix elements (variables or numbers):");
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++) {
                        System.out.print("Element [" + (i + 1) + "," + (j + 1) + "]: ");
                        String input = sc.nextLine().trim();
                        if (input.matches("-?\\d+(\\.\\d+)?")) matrix[i][j] = new Constant(Double.parseDouble(input));
                        else {
                            varsUsed.add(input);
                            matrix[i][j] = new Variable(input);
                        }
                    }
            } else {
                Random rnd = new Random();
                boolean includeSymbols = askYesNo(sc, "Do you want the matrix to contain symbolic variables?");

                System.out.println("Generating " + n + "x" + n + " matrix...");
                int varCounter = 1;
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++) {
                        if (includeSymbols && rnd.nextBoolean()) {
                            String varName = "x" + varCounter++;
                            matrix[i][j] = new Variable(varName);
                            varsUsed.add(varName);
                        } else {
                            matrix[i][j] = new Constant(rnd.nextInt(10) + 1);
                        }
                    }
            }
        } else {
            // Small matrix: always manual input
            System.out.println("Enter matrix elements (variables or numbers):");
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    System.out.print("Element [" + (i + 1) + "," + (j + 1) + "]: ");
                    String input = sc.nextLine().trim();
                    if (input.matches("-?\\d+(\\.\\d+)?")) matrix[i][j] = new Constant(Double.parseDouble(input));
                    else {
                        varsUsed.add(input);
                        matrix[i][j] = new Variable(input);
                    }
                }
        }

        Matrix m = new Matrix(matrix);
        m.printMatrix();

        // --- Check if matrix is purely numeric ---
        boolean allNumeric = true;
        for (int i = 0; i < n && allNumeric; i++)
            for (int j = 0; j < n && allNumeric; j++)
                if (matrix[i][j] instanceof Variable) allNumeric = false;

        if (n <= SYMBOLIC_LIMIT) {
            // Small matrix: recursive symbolic
            Symbol det = m.determinant(tracer, null);
            System.out.println("\nSymbolic determinant:");
            System.out.println(det.toExpr());

            if (!varsUsed.isEmpty()) {
                boolean calcNumericWithVars = askYesNo(sc, "Calculate numeric determinant with variables?");

                if (calcNumericWithVars) {
                    Map<String, Double> vars = new HashMap<>();
                    for (String v : varsUsed) {
                        System.out.print("Value for " + v + ": ");
                        vars.put(v, sc.nextDouble());
                    }
                    double numericDet = det.eval(vars);
                    System.out.println("Numeric determinant: " + numericDet);
                } else {
                    System.out.println("Skipped numeric evaluation.");
                }
            } else {
                boolean calcNumeric = askYesNo(sc, "Calculate numeric determinant?");

                if (calcNumeric) {
                    double numericDet = det.eval(new HashMap<>());
                    System.out.println("Numeric determinant: " + numericDet);
                } else System.out.println("Skipped numeric evaluation.");
            }

        } else if (allNumeric) {
            // Large numeric matrix: LU-based numeric
            double[][] numericData = new double[n][n];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    numericData[i][j] = matrix[i][j].eval(new HashMap<>());

            boolean detailedOutput = askYesNo(sc, "Detailed step-by-step numeric LU?");

            double numericDet = tracedNumericDeterminant(numericData, tracer, detailedOutput);
            System.out.println("\nNumeric determinant (LU-based): " + numericDet);

        } else {
            // Large matrix: symbolic LU
            boolean detailed = askYesNo(sc, "Detailed step-by-step symbolic LU?");
            Symbol det = symbolicLUDeterminant(m, tracer, detailed);
            System.out.println("\nSymbolic LU determinant:");
            System.out.println(det.toExpr());

            if (!varsUsed.isEmpty()) {
                Map<String, Double> vars = new HashMap<>();
                System.out.println("Enter numeric values for variables to evaluate determinant:");
                for (String v : varsUsed) {
                    System.out.print("Value for " + v + ": ");
                    vars.put(v, sc.nextDouble());
                }
                double numericDet = det.eval(vars);
                System.out.println("Numeric determinant: " + numericDet);
            } else {
                double numericDet = det.eval(new HashMap<>());
                System.out.println("Numeric determinant: " + numericDet);
            }
        }
    }
}