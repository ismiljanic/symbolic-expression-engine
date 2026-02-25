package symbolicdet.cli;

import java.util.Scanner;

public class NumericInputs {
    private double xValue;
    private double lValue;
    private double dValue;
    private double lambdaValue;

    public NumericInputs(double x, double l, double d, double lambda) {
        this.xValue = x;
        this.lValue = l;
        this.dValue = d;
        this.lambdaValue = lambda;
    }

    public static NumericInputs collectBaseInputs(Scanner sc, boolean needXL, boolean needD, boolean needLambda) {
        double x = 0, l = 0, d = 0, lambda = 0;

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
        if (needLambda) {
            System.out.print("Enter value for λ: ");
            lambda = sc.nextDouble();
            sc.nextLine();
        }

        return new NumericInputs(x, l, d, lambda);
    }

    public static int askInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " ");
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Invalid integer. Try again.");
            }
        }
    }

    public double getxValue() {
        return xValue;
    }

    public void setxValue(double xValue) {
        this.xValue = xValue;
    }

    public double getlValue() {
        return lValue;
    }

    public void setlValue(double lValue) {
        this.lValue = lValue;
    }

    public double getdValue() {
        return dValue;
    }

    public void setdValue(double dValue) {
        this.dValue = dValue;
    }

    public double getLambdaValue() {
        return lambdaValue;
    }

    public void setLambdaValue(double lambdaValue) {
        this.lambdaValue = lambdaValue;
    }
}
