package symbolicdet;

import symbolicdet.matrix.Matrix;
import symbolicdet.symbolic.expressions.Constant;
import symbolicdet.symbolic.expressions.Symbol;
import symbolicdet.symbolic.expressions.Variable;
import symbolicdet.utils.SymbolicTracer;

import java.io.IOException;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

import static symbolicdet.cli.NumericInputs.askInt;
import static symbolicdet.cli.YesNoPrompt.askYesNo;
import static symbolicdet.matrix.DeterminantService.runDeterminantProcess;
import static symbolicdet.matrix.Matrix.buildSpecialMatrix;
import static symbolicdet.symbolic.parser.ParseSymbolicInput.parseSymbolicInput;
import static symbolicdet.utils.ExtractVariablesFromMatrix.collectVars;

public class Main {

    static final MathContext MC = new MathContext(50, RoundingMode.HALF_UP);
    static Map<String, Double> persistentVars = new HashMap<>();
    static Matrix currentReduced = null;

    public static void main(String[] args) throws IOException {
        final int SYMBOLIC_LIMIT = 1;
        Scanner sc = new Scanner(System.in);
        SymbolicTracer tracer = new SymbolicTracer();

        System.out.print("Enter matrix size (n): ");
        int n = sc.nextInt();
        sc.nextLine();

        Matrix original;
        Set<String> varsUsed = new HashSet<>();

        if (n > SYMBOLIC_LIMIT) {
            boolean useSpecial = askYesNo(sc, "Use special tridiagonal symbolic matrix?");
            if (useSpecial) {
                original = buildSpecialMatrix(n);

                varsUsed.addAll(collectVars(original));

                varsUsed.add("lambda");
                varsUsed.add("d");
                varsUsed.add("x");
                varsUsed.add("l");

                System.out.println("\n=== Full symbolic matrix ===");
                original.printMatrixFull();
            } else {
                boolean manual = askYesNo(sc, "Do you want to enter matrix manually?");
                Symbol[][] matrix = new Symbol[n][n];

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
                                String varName = "x_l" + varCounter++;
                                matrix[i][j] = new Variable(varName);
                                varsUsed.add(varName);
                            } else {
                                matrix[i][j] = new Constant(rnd.nextInt(10) + 1);
                            }
                        }
                }
                original = new Matrix(matrix);
                System.out.println("\n=== Full symbolic matrix ===");
                original.printMatrix();
            }
        } else {
            Symbol[][] matrix = new Symbol[n][n];
            System.out.println("Enter matrix elements (variables or numbers):");
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    System.out.print("Element [" + (i + 1) + "," + (j + 1) + "]: ");
                    String input = sc.nextLine().trim();
                    matrix[i][j] = parseSymbolicInput(input, varsUsed);
                }
            original = new Matrix(matrix);

            System.out.println("\n=== Full symbolic matrix ===");
            original.printMatrix();
        }

        runDeterminantProcess(original, varsUsed, sc, tracer, persistentVars);

        while (askYesNo(sc, "Compute determinant of reduced matrix?")) {
            Matrix target = (currentReduced != null && currentReduced.getN() < original.getN())
                    ? askReducedOrOriginal(sc, original)
                    : original;

            if (target.getN() <= 1) {
                System.out.println("Matrix is 1x1 — cannot reduce further.");
                continue;
            }

            System.out.println("\nMatrix selected for reduction:");
            target.printMatrix();

            int row = askInt(sc, "Enter row index to remove (0-based): ");
            int col = askInt(sc, "Enter column index to remove (0-based): ");

            if (row < 0 || row >= target.getN() || col < 0 || col >= target.getN()) {
                System.out.println("Invalid indices. Try again.");
                continue;
            }

            currentReduced = target.subMatrixExcluding(row, col);

            varsUsed.addAll(collectVars(currentReduced));

            System.out.println("\nReduced matrix:");
            currentReduced.printMatrix();
            runDeterminantProcess(currentReduced, varsUsed, sc, tracer, persistentVars);
        }
    }

    private static Matrix askReducedOrOriginal(Scanner sc, Matrix original) {
        System.out.println("\nChoose matrix to reduce:");
        System.out.println("1) Current reduced matrix (" + currentReduced.getN() + "x" + currentReduced.getN() + ")");
        System.out.println("2) Original full matrix (" + original.getN() + "x" + original.getN() + ")");
        int choice = askInt(sc, "Enter your choice: ");
        return (choice == 1) ? currentReduced : original;
    }

    public static Map<String, Double> getPersistentVars() {
        return persistentVars;
    }

    public static void setPersistentVars(Map<String, Double> vars) {
        persistentVars = vars;
    }

    public static Matrix getCurrentReduced() {
        return currentReduced;
    }

    public static void setCurrentReduced(Matrix m) {
        currentReduced = m;
    }
}