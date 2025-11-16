import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    static final MathContext MC = new MathContext(50, RoundingMode.HALF_UP);

    // === Abstract Symbol ===
    static abstract class Symbol {
        abstract String toExpr();

        //smaller numbers
        abstract double eval(Map<String, Double> vars);

        //extremely big numbers
        abstract BigDecimal evalBD(Map<String, Double> vars);
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
            if (!vars.containsKey(name)) {
                System.out.println("MISSING: " + name);
                throw new RuntimeException("Missing value for variable: " + name);
            }
            return vars.get(name);
        }

        @Override
        public BigDecimal evalBD(Map<String, Double> vars) {
            if (!vars.containsKey(name))
                throw new RuntimeException("Missing var: " + name);

            return BigDecimal.valueOf(vars.get(name));
        }
    }

    // === Constant ===
    static class Constant extends Symbol {
        double value;

        Constant(double value) {
            this.value = value;
        }

        //        @Override
//        public String toExpr() {
//            return String.format(Locale.US, "%.6f", value);
//        }
        @Override
        public String toExpr() {
            // if exactly 1.0, don't print decimal form
            if (Math.abs(value - 1.0) < 1e-9) return "1";
            if (Math.abs(value + 1.0) < 1e-9) return "-1";
            // otherwise, print minimal decimal form
            return String.format(Locale.US, "%.6f", value)
                    .replaceAll("0+$", "")
                    .replaceAll("\\.$", "");
        }


        @Override
        public double eval(Map<String, Double> vars) {
            return value;
        }

        @Override
        public BigDecimal evalBD(Map<String, Double> vars) {
            return BigDecimal.valueOf(value);
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

        //        @Override
//        public String toExpr() {
//            return "(" + left.toExpr() + " " + op + " " + right.toExpr() + ")";
//        }
        @Override
        public String toExpr() {
            String leftExpr = left.toExpr();
            String rightExpr = right.toExpr();

            // Always ensure proper spacing between operands
            if (op == '*') {// Guard against accidental merges like "(l+1)1.000000"
                if (!leftExpr.endsWith(" ") && !leftExpr.endsWith("*"))
                    leftExpr += " ";
                if (!rightExpr.startsWith(" "))
                    rightExpr = " " + rightExpr;
                return "(" + leftExpr + "*" + rightExpr + ")";
            }
            return "(" + leftExpr + " " + op + " " + rightExpr + ")";
        }


        @Override
        public double eval(Map<String, Double> vars) {
            double a = left.eval(vars);
            double b = right.eval(vars);
            return switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }

        @Override
        public BigDecimal evalBD(Map<String, Double> vars) {
            BigDecimal a = left.evalBD(vars);
            BigDecimal b = right.evalBD(vars);

            return switch (op) {
                case '+' -> a.add(b);
                case '-' -> a.subtract(b);
                case '*' -> a.multiply(b);
                case '/' -> a.divide(b, MathContext.DECIMAL128);
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
                Symbol term = simplify(multiplyIfNotZero(sign, multiplyIfNotZero(elem, minorDet)));

                if (!isZero(term)) {
                    if (numericVars != null) {
                        tracer.log("Numeric value of this term: " + term.eval(numericVars));
                    }
                    tracer.log("Symbolic term to add: " + term.toExpr() + "\n");
                    det.set(new Expression(det.get(), '+', term));
                } else {
                    tracer.log("Skipping zero term: " + term.toExpr());
                }
            }
            return det.get();
        }

        private static Symbol simplify(Symbol sym) {
            if (sym instanceof Constant || sym instanceof Variable || sym == null) {
                return sym; // nothing to simplify
            }

            if (sym instanceof Expression e) {
                e.left = simplify(e.left);
                e.right = simplify(e.right);

                switch (e.op) {
                    case '*':
                        if (isZero(e.left) || isZero(e.right)) return new Constant(0);
                        if (isOne(e.left)) return e.right;
                        if (isOne(e.right)) return e.left;
                        break;
                    case '+':
                        if (isZero(e.left)) return e.right;
                        if (isZero(e.right)) return e.left;
                        break;
                    case '-':
                        if (isZero(e.left)) return new Expression(new Constant(0), '-', e.right);
                        if (isZero(e.right)) return e.left;
                        break;
                    case '/':
                        if (isZero(e.left)) return new Constant(0);
                        if (isOne(e.right)) return e.left;
                        break;
                }
                return e;
            }

            // fallback for unexpected types
            return sym;
        }

        private static boolean isOne(Symbol s) {
            return s instanceof Constant c && Math.abs(c.value - 1.0) < 1e-12;
        }

        private static Symbol multiplyIfNotZero(Symbol a, Symbol b) {
            if (isZero(a) || isZero(b)) return new Constant(0);
            return new Expression(a, '*', b);
        }

        private static boolean isZero(Symbol sym) {
            if (sym instanceof Constant c) {
                return Math.abs(c.value) < 1e-12;
            } else if (sym instanceof Expression e) {
                if (e.op == '*') {
                    return isZero(e.left) || isZero(e.right);
                } else if (e.op == '+' || e.op == '-') {
                    return isZero(e.left) && isZero(e.right);
                } else if (e.op == '/') {
                    return isZero(e.left); // numerator zero → zero
                }
            }
            return false;
        }

        void printMatrix() {
            int colWidth = 30;

            System.out.print("      ");
            for (int j = 0; j < n; j++) {
                System.out.printf("%-" + colWidth + "s", "Col " + j);
            }
            System.out.println();

            System.out.print("      ");
            for (int j = 0; j < n; j++) System.out.print("-".repeat(colWidth));
            System.out.println();

            for (int i = 0; i < n; i++) {
                System.out.printf("Row %-3d", i);
                for (int j = 0; j < n; j++) {
                    String expr = data[i][j].toExpr();
                    if (expr.length() > colWidth - 1) expr = expr.substring(0, colWidth - 4) + "...";
                    System.out.printf("%-" + colWidth + "s", expr);
                }
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

        //new matrix without specific row and column
        Matrix removeRowCol(int removeRow, int removeCol) {
            Symbol[][] newData = new Symbol[n - 1][n - 1]; // size reduced for exlcuded row and column

            int r2 = 0;
            for (int r = 0; r < n; r++) {
                if (r == removeRow) continue;

                int c2 = 0;
                for (int c = 0; c < n; c++) {
                    if (c == removeCol) continue;

                    newData[r2][c2] = data[r][c];
                    c2++;
                }
                r2++;
            }
            return new Matrix(newData, name + "_rm" + removeRow + "_" + removeCol);
        }
    }

    // === Numeric LU determinant with step-by-step trace ===
    static BigDecimal tracedNumericDeterminant(BigDecimal[][] mat, SymbolicTracer tracer, boolean detailedOutput) {
        int n = mat.length;
        BigDecimal det = BigDecimal.ONE;
        MathContext mc = MathContext.DECIMAL128;

        tracer.log("Starting LU-based determinant calculation for " + n + "x" + n + " matrix.");
        tracer.log("Initial matrix:");
        printNumericMatrix(mat, tracer);

        for (int i = 0; i < n; i++) {
            // Find pivot
            int pivot = i;
            BigDecimal maxPivot = mat[i][i].abs();
            for (int r = i + 1; r < n; r++) {
                BigDecimal absVal = mat[r][i].abs();
                if (absVal.compareTo(maxPivot) > 0) {
                    pivot = r;
                    maxPivot = absVal;
                }
            }
            if (detailedOutput)
                tracer.log("Selecting pivot for column " + i + ": row " + pivot + " (value = " + mat[pivot][i] + ")");

            if (mat[pivot][i].compareTo(BigDecimal.ZERO) == 0) {
                tracer.log("Pivot is zero. Determinant is 0.");
                return BigDecimal.ZERO;
            }

            // Swap rows if necessary
            if (i != pivot) {
                BigDecimal[] tmp = mat[i];
                mat[i] = mat[pivot];
                mat[pivot] = tmp;
                det = det.negate();
                if (detailedOutput) {
                    tracer.log("Swapped rows " + i + " and " + pivot + ". Updated det sign.");
                    tracer.log("Matrix after row swap:");
                    printNumericMatrix(mat, tracer);
                }
            }

            det = det.multiply(mat[i][i], mc);
            if (detailedOutput)
                tracer.log("Pivot [" + i + "," + i + "] = " + mat[i][i] + ". Partial determinant = " + det);

            for (int r = i + 1; r < n; r++) {
                BigDecimal factor = mat[r][i].divide(mat[i][i], mc);
                for (int c = i; c < n; c++) {
                    mat[r][c] = mat[r][c].subtract(factor.multiply(mat[i][c], mc), mc);
                    if (detailedOutput)
                        tracer.log("Eliminating row " + r + " using factor (" + mat[r][c] + " / " + mat[c][c] + ") = " + factor + " * row " + c);
                }
            }

            if (detailedOutput) {
                tracer.log("Matrix after eliminating column " + i + ":");
                printNumericMatrix(mat, tracer);
            }
        }

        if (detailedOutput) tracer.log("Finished LU elimination. Determinant = " + det);
        return det;
    }

    static void printNumericMatrix(BigDecimal[][] mat, SymbolicTracer tracer) {
        for (BigDecimal[] row : mat) {
            StringBuilder sb = new StringBuilder();
            for (BigDecimal val : row) sb.append(String.format("%10.4f", val)).append(" ");
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

        if (detailedOutput)
            tracer.log("Starting symbolic LU-based determinant calculation for " + n + "x" + n + " matrix.");

        for (int i = 0; i < n; i++) {
            int pivot = i;
            while (pivot < n && mat[pivot][i].toExpr().equals("0")) pivot++;

            if (pivot == n) {
                if (detailedOutput) tracer.log("Entire column " + i + " is zero. Determinant = 0.");
                return new Constant(0);
            }

            if (pivot != i) {
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

            det = new Expression(det, '*', mat[i][i]);
            if (detailedOutput) tracer.log("Partial determinant after multiplying by pivot: " + det.toExpr());

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

        det = new Expression(detSign, '*', det);
        if (detailedOutput) tracer.log("Final symbolic determinant: " + det.toExpr());

        return det;
    }

    private static void printSymbolicMatrix(Symbol[][] mat, SymbolicTracer tracer) {
        tracer.log("Current matrix state:");
        for (Symbol[] row : mat) {
            StringBuilder sb = new StringBuilder();
            for (Symbol s : row) sb.append(s.toExpr()).append("\t");
            tracer.log(sb.toString());
        }
        tracer.log("");
    }

    private static boolean askYesNo(Scanner sc, String prompt) {
        String input;
        while (true) {
            System.out.print(prompt + " (yes/no): ");
            input = sc.nextLine().trim().toLowerCase();
            if (input.isEmpty()) continue;
            if (input.equals("yes")) return true;
            if (input.equals("no")) return false;
            System.out.println("Invalid input. Please type 'yes' or 'no'.");
        }
    }


    // === Data holder for base numeric inputs ===
    static class NumericInputs {
        double xValue;
        double lValue;
        double dValue;

        NumericInputs(double x, double l, double d) {
            this.xValue = x;
            this.lValue = l;
            this.dValue = d;
        }

    }

    // --- 1. Collect required user inputs ---
    private static NumericInputs collectBaseInputs(Scanner sc, boolean needXL, boolean needD) {
        double x = 0, l = 0, d = 0;

        if (needXL) {
            System.out.print("Enter value for x: ");
            x = sc.nextDouble();
            sc.nextLine();
            System.out.print("Enter value for l: ");
            l = sc.nextDouble();
            sc.nextLine();
        }
        if (needD) {
            System.out.print("Enter value for d: ");
            d = sc.nextDouble();
            sc.nextLine();
        }

        return new NumericInputs(x, l, d);
    }

    // --- 2. Compute offset from variable like x_l+1 or x_l-2 ---
    private static int getOffset(String var) {
        int plusIndex = var.indexOf('+');
        int minusIndex = var.indexOf('-');

        if (plusIndex > 0) return Integer.parseInt(var.substring(plusIndex + 1));
        if (minusIndex > 0) return -Integer.parseInt(var.substring(minusIndex + 1));
        return 0;
    }

    // --- 5. Populate t_l variables (t_l = x_l * x_{l+1} - D) ---
    private static void populateTLVars(Map<String, Double> vars, List<String> tLVars, NumericInputs inputs) {
        for (String v : tLVars) {
            int offset = getOffset(v); // e.g., -2, -1, +1, 0
            double lShifted = inputs.lValue + offset;

            // compute x_{l+n} and x_{l+n+1}
            double x_l_val = inputs.xValue - 2 * (lShifted - 1) * lShifted;
            double x_l_plus1_val = inputs.xValue - 2 * (lShifted + 1 - 1) * (lShifted + 1);
            double t_val = (x_l_val * x_l_plus1_val) - inputs.dValue;
            vars.put(v, t_val);

            System.out.printf("Computed %s = (%f * %f) - %f = %f%n",
                    v, x_l_val, x_l_plus1_val, inputs.dValue, t_val);
        }
    }

    private static void populateXLVar(Map<String, Double> vars, String v, NumericInputs inputs) {
        int offset = getOffset(v);
        double lShifted = inputs.lValue + offset;
        double x_l_val = inputs.xValue - 2 * (lShifted - 1) * lShifted;
        vars.put(v, x_l_val);
    }

    private static void populateTLVar(Map<String, Double> vars, String v, NumericInputs inputs) {
        int offset = getOffset(v);
        double lShifted = inputs.lValue + offset;
        double x_l_val = inputs.xValue - 2 * (lShifted - 1) * lShifted;
        double x_l_plus1_val = inputs.xValue - 2 * (lShifted + 1 - 1) * (lShifted + 1);
        vars.put(v, x_l_val * x_l_plus1_val - inputs.dValue);
    }

    // --- 6. Handle user-defined variables (not x_l / t_l) ---
    private static void populateRemainingVars(Scanner sc, Map<String, Double> vars, Set<String> varsUsed) {
        for (String v : varsUsed) {
            if (vars.containsKey(v)) continue;

            boolean isNegative = v.startsWith("-");
            String base = isNegative ? v.substring(1) : v;

            System.out.print("Value for " + base + ": ");
            double val = sc.nextDouble();
            vars.put(v, isNegative ? -val : val);
        }
    }

    private static BigDecimal[][] substituteNumericMatrix(Matrix m, Map<String, Double> vars) {
        BigDecimal[][] numeric = new BigDecimal[m.n][m.n];

        for (int i = 0; i < m.n; i++) {
            for (int j = 0; j < m.n; j++) {
                numeric[i][j] = m.data[i][j].evalBD(vars);
            }
        }
        return numeric;
    }

    private static Symbol parseSymbolicInput(String input, Set<String> varsUsed) {
        input = input.replaceAll("\\s+", "");
        AtomicInteger pos = new AtomicInteger(0);
        return parseExpression(input, pos, varsUsed);
    }

    private static Symbol parseExpression(String input, AtomicInteger pos, Set<String> varsUsed) {
        Symbol term = parseTerm(input, pos, varsUsed);
        while (pos.get() < input.length()) {
            char op = input.charAt(pos.get());
            if (op != '+' && op != '-') break;
            pos.incrementAndGet();
            Symbol nextTerm = parseTerm(input, pos, varsUsed);
            term = new Expression(term, op, nextTerm);
        }
        return term;
    }

    private static Symbol parseTerm(String input, AtomicInteger pos, Set<String> varsUsed) {
        Symbol factor = parseFactor(input, pos, varsUsed);
        while (pos.get() < input.length()) {
            char op = input.charAt(pos.get());
            if (op != '*' && op != '/') break;
            pos.incrementAndGet();
            Symbol nextFactor = parseFactor(input, pos, varsUsed);
            factor = new Expression(factor, op, nextFactor);
        }
        return factor;
    }

    private static Symbol parseFactor(String input, AtomicInteger pos, Set<String> varsUsed) {
        if (pos.get() >= input.length()) return null;

        char c = input.charAt(pos.get());
        if (c == '(') {
            pos.incrementAndGet();
            Symbol expr = parseExpression(input, pos, varsUsed);
            if (pos.get() >= input.length() || input.charAt(pos.get()) != ')')
                throw new RuntimeException("Mismatched parentheses");
            pos.incrementAndGet();
            return expr;
        }

        if (c == '-') {
            pos.incrementAndGet();
            Symbol inner = parseFactor(input, pos, varsUsed);
            return new Expression(new Constant(-1), '*', inner);
        }

        int start = pos.get();
        while (pos.get() < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos.get())) ||
                        input.charAt(pos.get()) == '_' ||
                        input.charAt(pos.get()) == '+' ||
                        input.charAt(pos.get()) == '-' ||
                        input.charAt(pos.get()) == '.')) {
            pos.incrementAndGet();
        }

        String token = input.substring(start, pos.get());
        if (token.matches("\\d+(\\.\\d+)?")) return new Constant(Double.parseDouble(token.replace(',', '.')));
        varsUsed.add(token);
        return new Variable(token);
    }

    private static Symbol[][] getPredefinedTestMatrix4(Set<String> varsUsed) {
        Symbol[][] m = new Symbol[4][4];

        m[0][0] = parseSymbolicInput("6*(x_l-2)+3*(x_l+1)", varsUsed);
        m[0][1] = parseSymbolicInput("3*t_l", varsUsed);
        m[0][2] = parseSymbolicInput("0", varsUsed);
        m[0][3] = parseSymbolicInput("-1*(t_l-2)*(t_l-1)*t_l", varsUsed);

        m[1][0] = parseSymbolicInput("-3", varsUsed);
        m[1][1] = parseSymbolicInput("6*(x_l-2)-2*x_l", varsUsed);
        m[1][2] = parseSymbolicInput("t_l-1", varsUsed);
        m[1][3] = parseSymbolicInput("(t_l-2)*(t_l-1)*(x_l+1)", varsUsed);

        m[2][0] = parseSymbolicInput("0", varsUsed);
        m[2][1] = parseSymbolicInput("-5", varsUsed);
        m[2][2] = parseSymbolicInput("6*(x_l-2)-5*(x_l-1)", varsUsed);
        m[2][3] = parseSymbolicInput("-1*(t_l-2)*((x_l)*(x_l+1)+(d/3)) ", varsUsed);

        m[3][0] = parseSymbolicInput("0", varsUsed);
        m[3][1] = parseSymbolicInput("0", varsUsed);
        m[3][2] = parseSymbolicInput("-6", varsUsed);
        m[3][3] = parseSymbolicInput("(x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2)", varsUsed);
        return m;
    }

    private static Symbol[][] getPredefinedTestMatrix(Set<String> varsUsed) {
        Symbol[][] m = new Symbol[5][5];

        m[0][0] = parseSymbolicInput("10*(x_l-3) + 6*(x_l+1)", varsUsed);
        m[0][1] = parseSymbolicInput("6*t_l", varsUsed);
        m[0][2] = parseSymbolicInput("0", varsUsed);
        m[0][3] = parseSymbolicInput("0", varsUsed);
        m[0][4] = parseSymbolicInput("(t_l-3)*(t_l-2)*(t_l-1)*t_l", varsUsed);

        m[1][0] = parseSymbolicInput("-4", varsUsed);
        m[1][1] = parseSymbolicInput("10*(x_l-3) - 1*(x_l)", varsUsed);
        m[1][2] = parseSymbolicInput("3*(t_l-1)", varsUsed);
        m[1][3] = parseSymbolicInput("0", varsUsed);
        m[1][4] = parseSymbolicInput("-1*(t_l-3)*(t_l-2)*(t_l-1)*(x_l+1)", varsUsed);

        m[2][0] = parseSymbolicInput("0", varsUsed);
        m[2][1] = parseSymbolicInput("-7", varsUsed);
        m[2][2] = parseSymbolicInput("10*(x_l-3) - 6*(x_l-1)", varsUsed);
        m[2][3] = parseSymbolicInput("t_l-2", varsUsed);
        m[2][4] = parseSymbolicInput("(t_l-3)*(t_l-2)*((x_l)*(x_l+1)+(d/3))", varsUsed);

        m[3][0] = parseSymbolicInput("0", varsUsed);
        m[3][1] = parseSymbolicInput("0", varsUsed);
        m[3][2] = parseSymbolicInput("-9", varsUsed);
        m[3][3] = parseSymbolicInput("10*(x_l-3) - 9*(x_l-2)", varsUsed);
        m[3][4] = parseSymbolicInput("-1*(t_l-3)*((x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2))", varsUsed);

        m[4][0] = parseSymbolicInput("0", varsUsed);
        m[4][1] = parseSymbolicInput("0", varsUsed);
        m[4][2] = parseSymbolicInput("0", varsUsed);
        m[4][3] = parseSymbolicInput("-10", varsUsed);
        m[4][4] = parseSymbolicInput("(x_l-2)*(x_l-1)*x_l*(x_l+1)+(((5*(x_l+1))+(3*(x_l-3)))*((x_l-2)+x_l)+(6*(x_l-1)*x_l)-2*(x_l-3)*(x_l-2)+2*d)*(d/10)", varsUsed);

        return m;
    }

    private static String expandSymbolic(String expr) {
        // handle x_l±n — wrap all replacements in parentheses so outer multipliers apply to the whole
        expr = expr.replaceAll("x_l\\+?(\\d+)", "(x - 2*((l+$1)-1)*(l+$1))");
        expr = expr.replaceAll("x_l-(\\d+)", "(x - 2*((l-$1)-1)*(l-$1))");
        expr = expr.replaceAll("x_l(?![+-])", "(x - 2*(l - 1)*l)");

        // handle t_l±n — same rule: always wrap fully in parentheses
        expr = expr.replaceAll("t_l\\+?(\\d+)", "((x - 2*((l+$1)-1)*(l+$1)) * (x - 2*((l+$1+1)-1)*(l+$1+1)) - d)");
        expr = expr.replaceAll("t_l-(\\d+)", "((x - 2*((l-$1)-1)*(l-$1)) * (x - 2*((l-$1+1)-1)*(l-$1+1)) - d)");
        expr = expr.replaceAll("t_l(?![+-])", "((x - 2*(l - 1)*l) * (x - 2*((l+1)-1)*(l+1)) - d)");

        // normalize redundant operators
        expr = expr.replaceAll("--", "+");
        expr = expr.replaceAll("\\+\\+", "+");
        expr = expr.replaceAll("\\+-", "-");
        expr = expr.replaceAll("-\\+", "-");

        return expr;
    }


    private static String simplifySymbolic(String expr) {
        expr = expr.replaceAll("\\s+", ""); // remove spaces

        // Replace ^ with Math.pow
        expr = expr.replaceAll("(\\w+)\\^(\\d+)", "Math.pow($1,$2)");

        // Replace 'l' and 'x' and 'D' with symbols for safety
        // (we'll just expand algebraically, not evaluate numerically)
        // Expand arithmetic patterns like (l+1)-1 → l
        expr = expr.replaceAll("\\(l\\+1\\)-1", "l");
        expr = expr.replaceAll("\\(l-1\\)\\+1", "l");
        expr = expr.replaceAll("\\(l\\+0\\)", "l");
        expr = expr.replaceAll("\\(l-0\\)", "l");

        // Expand (l+1)*(l+1) → l*l + 2*l + 1
        expr = expr.replaceAll("\\(l\\+1\\)\\*(l\\+1\\))", "(l*l + 2*l + 1)");
        expr = expr.replaceAll("\\(l-1\\)\\*(l-1\\))", "(l*l - 2*l + 1)");
        expr = expr.replaceAll("\\(l\\+1\\)\\*(l-1\\))", "(l*l - 1)");
        expr = expr.replaceAll("\\(l-1\\)\\*(l\\+1\\))", "(l*l - 1)");


        // Expand nested (l+n)-1 and (l-n)-1 patterns generally
        expr = expr.replaceAll("\\(l\\+([0-9]+)\\)-1", "(l+$1-1)");
        expr = expr.replaceAll("\\(l-([0-9]+)\\)-1", "(l-$1-1)");

        // Simplify *1, 1*, etc.
        expr = expr.replaceAll("\\*1(?!\\d)", "");
        expr = expr.replaceAll("1\\*", "");

        // Simplify double negatives
        expr = expr.replaceAll("--", "+");
        expr = expr.replaceAll("\\+-", "-");
        expr = expr.replaceAll("-\\+", "-");

        return expr;
    }

    private static boolean isNumeric(Symbol s) {
        if (s instanceof Constant) return true;
        if (s instanceof Variable) return false;
        if (s instanceof Expression e) return isNumeric(e.left) && isNumeric(e.right);
        return false;
    }


    static Map<String, Double> persistentVars = new HashMap<>();
    static Matrix currentReduced = null;

    static void runDeterminantProcess(
            Matrix m,
            Set<String> varsUsed,
            Scanner sc,
            SymbolicTracer tracer
    ) {
        m.printMatrix();

        boolean allNumeric = true;
        for (int i = 0; i < m.n && allNumeric; i++) {
            for (int j = 0; j < m.n && allNumeric; j++) {
                Symbol s = m.data[i][j];
                if (!isNumeric(s)) {
                    System.out.println("Contains variable/expression: " + s.toExpr());
                    allNumeric = false;
                }
            }
        }

        if (!allNumeric) {
            Symbol det = m.determinant(tracer, null);
            String expr = det.toExpr();
            System.out.println("\nSymbolic LU determinant:");
            System.out.println(expr);

            String expanded = expandSymbolic(expr);
            String simplified = simplifySymbolic(expanded);
            String expandedPoly = PolySimplifier.expandSymbolicOnly(simplified);
            System.out.println("\nFinal expanded polynomial:");
            System.out.println(expandedPoly);

            if (!askYesNo(sc, "Calculate numeric determinant with variables?")) return;

            Map<String, Double> vars = new HashMap<>(persistentVars);
            vars.clear();

            List<String> xLVars = varsUsed.stream().filter(v -> v.matches("x_l([+-]\\d+)?")).toList();
            List<String> tLVars = varsUsed.stream().filter(v -> v.matches("t_l([+-]\\d+)?")).toList();

            boolean needXL = !xLVars.isEmpty() || !tLVars.isEmpty();
            boolean needD = varsUsed.contains("d") || !tLVars.isEmpty();

            NumericInputs inputs = collectBaseInputs(sc, needXL, needD);
            vars.put("x", inputs.xValue);
            vars.put("l", inputs.lValue);
            vars.put("d", inputs.dValue);

            for (String v : xLVars) if (!vars.containsKey(v)) populateXLVar(vars, v, inputs);
            for (String v : tLVars) if (!vars.containsKey(v)) populateTLVar(vars, v, inputs);

            for (String v : varsUsed)
                if (!vars.containsKey(v)) {
                    System.out.print("Value for " + v + ": ");
                    double val = sc.nextDouble();
                    vars.put(v, val);
                }

            persistentVars.putAll(vars);

            BigDecimal numericFromSymbolic = det.evalBD(vars);
            System.out.printf("Numeric determinant (symbolic substitution): %.6f%n", numericFromSymbolic);

            BigDecimal[][] numericData = substituteNumericMatrix(m, vars);
            boolean detailedNumeric = askYesNo(sc, "Run numeric LU with these values for correct pivoting?");
            BigDecimal numericDetLU = tracedNumericDeterminant(numericData, tracer, detailedNumeric);
            System.out.printf("Numeric determinant (LU-based): %.6f%n", numericDetLU);

            return;
        } else {
            BigDecimal[][] numericData = new BigDecimal[m.n][m.n];
            for (int i = 0; i < m.n; i++) {
                for (int j = 0; j < m.n; j++) {
                    numericData[i][j] = BigDecimal.valueOf(m.data[i][j].eval(new HashMap<>()));
                }
            }
            boolean detailedOutput = askYesNo(sc, "Detailed step-by-step numeric LU?");
            BigDecimal numericDet = tracedNumericDeterminant(numericData, tracer, detailedOutput);
            System.out.printf("Numeric determinant (LU based): %.6f%n", numericDet);
        }

        repeatWithReducedMatrix(m, varsUsed, sc, tracer);
    }

    private static void repeatWithReducedMatrix(
            Matrix original,
            Set<String> varsUsed,
            Scanner sc,
            SymbolicTracer tracer
    ) {
        if (currentReduced == null) currentReduced = original;

        while (askYesNo(sc, "Reduce a matrix further?")) {
            Matrix target;

            if (currentReduced != null && currentReduced.n < original.n) {
                // Offer choice only if we already have a reduced matrix
                System.out.println("\nChoose matrix to reduce:");
                System.out.println("1) Current reduced matrix (" + currentReduced.n + "x" + currentReduced.n + ")");
                System.out.println("2) Original full matrix (" + original.n + "x" + original.n + ")");
                int choice = askInt(sc, "");
                target = (choice == 1) ? currentReduced : original;
            } else {
                target = currentReduced; // first reduction
            }

            if (target.n <= 1) {
                System.out.println("Matrix is 1x1 — cannot reduce further.");
                continue;
            }

            System.out.println("\nReducing this matrix (" + target.n + "x" + target.n + "):");
            target.printMatrix();

            int row = askInt(sc, "Enter row index to remove (0-based): ");
            int col = askInt(sc, "Enter column index to remove (0-based): ");
            if (row < 0 || row >= target.n || col < 0 || col >= target.n) {
                System.out.println("Invalid indices. Try again.");
                continue;
            }

            currentReduced = target.removeRowCol(row, col);

            // Keep varsUsed from previous matrices intact
            varsUsed.addAll(collectVars(currentReduced));

            System.out.println("\n--- New Reduced Matrix (" + currentReduced.n + "x" + currentReduced.n + ") ---");
            currentReduced.printMatrix();

            runDeterminantProcess(currentReduced, varsUsed, sc, tracer);
        }
    }

    // Extract variables from a matrix
    private static Set<String> collectVars(Matrix m) {
        Set<String> vars = new HashSet<>();
        for (int i = 0; i < m.n; i++)
            for (int j = 0; j < m.n; j++)
                if (m.data[i][j] instanceof Variable v) vars.add(v.name);
        return vars;
    }

    private static int askInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " ");
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Invalid integer. Try again.");
            }
        }
    }


    public static void main(String[] args) {
        final int SYMBOLIC_LIMIT = 2;
        Scanner sc = new Scanner(System.in);
        SymbolicTracer tracer = new SymbolicTracer();

        System.out.print("Enter matrix size (n): ");
        int n = sc.nextInt();
        sc.nextLine();

        Symbol[][] matrix = new Symbol[n][n];
        Set<String> varsUsed = new HashSet<>();

        if (n == 5) {
            System.out.println("Loaded predefined 5x5 test matrix...");
            matrix = getPredefinedTestMatrix(varsUsed);
        } else if (n == 4) {
            System.out.println("Loaded predefined 4x4 test matrix...");
            matrix = getPredefinedTestMatrix4(varsUsed);
        } else if (n > SYMBOLIC_LIMIT) {
            boolean manual = askYesNo(sc, "Do you want to enter matrix manually?");

            if (manual) {
                System.out.println("Enter matrix elements (variables or numbers):");
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++) {
                        System.out.print("Element [" + (i + 1) + "," + (j + 1) + "]: ");
                        String input = sc.nextLine().trim();
                        matrix[i][j] = parseSymbolicInput(input, varsUsed);
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
            System.out.println("Enter matrix elements (variables or numbers):");
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    System.out.print("Element [" + (i + 1) + "," + (j + 1) + "]: ");
                    String input = sc.nextLine().trim();
                    matrix[i][j] = parseSymbolicInput(input, varsUsed);
                }
        }

        Matrix m = new Matrix(matrix);
        m.printMatrix();

        runDeterminantProcess(m, varsUsed, sc, tracer);

        while (askYesNo(sc, "Compute determinant of reduced matrix?")) {
            m.printMatrix();

            System.out.print("Available row indices: ");
            for (int i = 0; i < m.n; i++) System.out.print(i + " ");
            System.out.println();

            System.out.print("Available column indices: ");
            for (int i = 0; i < m.n; i++) System.out.print(i + " ");
            System.out.println();

            int row = askInt(sc, "Enter row index to remove (0-based): ");
            int col = askInt(sc, "Enter column index to remove (0-based): ");

            if (row < 0 || row >= m.n || col < 0 || col >= m.n) {
                System.out.println("Invalid indices. Try again.");
                continue;
            }

            m = m.removeRowCol(row, col);
            varsUsed.addAll(collectVars(m));
            System.out.println("\nReduced matrix:");
            m.printMatrix();

            runDeterminantProcess(m, varsUsed, sc, tracer);
        }
    }
}