package symbolicdet.cli;

import symbolicdet.matrix.Matrix;
import symbolicdet.utils.SymbolicTracer;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static symbolicdet.cli.NumericInputs.askInt;
import static symbolicdet.cli.YesNoPrompt.askYesNo;
import static symbolicdet.matrix.DeterminantService.runDeterminantProcess;
import static symbolicdet.utils.ExtractVariablesFromMatrix.collectVars;

public class MatrixPrompt {
    public static void repeatWithReducedMatrix(Matrix original, Set<String> varsUsed, Scanner sc, SymbolicTracer tracer, Map<String, Double> persistentVars, Matrix currentReduced) throws IOException {
        if (currentReduced == null) currentReduced = original;

        while (askYesNo(sc, "Reduce a matrix further?")) {
            Matrix target;

            if (currentReduced != null && currentReduced.getN() < original.getN()) {
                // Offer choice only if we already have a reduced matrix
                System.out.println("\nChoose matrix to reduce:");
                System.out.println("1) Current reduced matrix (" + currentReduced.getN() + "x" + currentReduced.getN() + ")");
                System.out.println("2) Original full matrix (" + original.getN() + "x" + original.getN() + ")");
                int choice = askInt(sc, "");
                target = (choice == 1) ? currentReduced : original;
            } else {
                target = currentReduced; // first reduction
            }

            if (target.getN() <= 1) {
                System.out.println("Matrix is 1x1 — cannot reduce further.");
                continue;
            }

            System.out.println("\nReducing this matrix (" + target.getN() + "x" + target.getN() + "):");
            target.printMatrix();

            int row = askInt(sc, "Enter row index to remove (0-based): ");
            int col = askInt(sc, "Enter column index to remove (0-based): ");
            if (row < 0 || row >= target.getN() || col < 0 || col >= target.getN()) {
                System.out.println("Invalid indices. Try again.");
                continue;
            }

            currentReduced = target.subMatrixExcluding(row, col);

            // Keep varsUsed from previous matrices intact
            varsUsed.addAll(collectVars(currentReduced));

            System.out.println("\n--- New Reduced Matrix (" + currentReduced.getN() + "x" + currentReduced.getN() + ") ---");
            currentReduced.printMatrix();

            runDeterminantProcess(currentReduced, varsUsed, sc, tracer, persistentVars);
        }
    }
}
