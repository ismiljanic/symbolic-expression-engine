package symbolicdet.matrix;

import symbolicdet.symbolic.expressions.Symbol;

import java.util.Set;

import static symbolicdet.symbolic.parser.ParseSymbolicInput.parseSymbolicInput;

public class PredefinedMatrices {
    //4x4 matrix
    public static Symbol[][] getPredefinedTestMatrix4(Set<String> varsUsed) {
        Symbol[][] matrix = new Symbol[4][4];

        matrix[0][0] = parseSymbolicInput("6*(x_l-2)+3*(x_l+1) - lambda", varsUsed);
        matrix[0][1] = parseSymbolicInput("3*t_l", varsUsed);
        matrix[0][2] = parseSymbolicInput("0", varsUsed);
        matrix[0][3] = parseSymbolicInput("-1*(t_l-2)*(t_l-1)*t_l", varsUsed);

        matrix[1][0] = parseSymbolicInput("-3", varsUsed);
        matrix[1][1] = parseSymbolicInput("6*(x_l-2)-2*(x_l) - lambda", varsUsed);
        matrix[1][2] = parseSymbolicInput("t_l-1", varsUsed);
        matrix[1][3] = parseSymbolicInput("(t_l-2)*(t_l-1)*(x_l+1)", varsUsed);

        matrix[2][0] = parseSymbolicInput("0", varsUsed);
        matrix[2][1] = parseSymbolicInput("-5", varsUsed);
        matrix[2][2] = parseSymbolicInput("6*(x_l-2)-5*(x_l-1) - lambda", varsUsed);
        matrix[2][3] = parseSymbolicInput("-1*(t_l-2)*((x_l)*(x_l+1)+(d/3)) ", varsUsed);

        matrix[3][0] = parseSymbolicInput("0", varsUsed);
        matrix[3][1] = parseSymbolicInput("0", varsUsed);
        matrix[3][2] = parseSymbolicInput("-6", varsUsed);
        matrix[3][3] = parseSymbolicInput("(x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2) - lambda", varsUsed);
        return matrix;
    }

    //5x5 matrix
    public static Symbol[][] getPredefinedTestMatrix5(Set<String> varsUsed) {
        Symbol[][] matrix = new Symbol[5][5];

        matrix[0][0] = parseSymbolicInput("10*(x_l-3) + 6*(x_l+1)", varsUsed);
        matrix[0][1] = parseSymbolicInput("6*t_l", varsUsed);
        matrix[0][2] = parseSymbolicInput("0", varsUsed);
        matrix[0][3] = parseSymbolicInput("0", varsUsed);
        matrix[0][4] = parseSymbolicInput("(t_l-3)*(t_l-2)*(t_l-1)*t_l", varsUsed);

        matrix[1][0] = parseSymbolicInput("-4", varsUsed);
        matrix[1][1] = parseSymbolicInput("10*(x_l-3) - 1*(x_l)", varsUsed);
        matrix[1][2] = parseSymbolicInput("3*(t_l-1)", varsUsed);
        matrix[1][3] = parseSymbolicInput("0", varsUsed);
        matrix[1][4] = parseSymbolicInput("-1*(t_l-3)*(t_l-2)*(t_l-1)*(x_l+1)", varsUsed);

        matrix[2][0] = parseSymbolicInput("0", varsUsed);
        matrix[2][1] = parseSymbolicInput("-7", varsUsed);
        matrix[2][2] = parseSymbolicInput("10*(x_l-3) - 6*(x_l-1)", varsUsed);
        matrix[2][3] = parseSymbolicInput("t_l-2", varsUsed);
        matrix[2][4] = parseSymbolicInput("(t_l-3)*(t_l-2)*((x_l)*(x_l+1)+(d/3))", varsUsed);

        matrix[3][0] = parseSymbolicInput("0", varsUsed);
        matrix[3][1] = parseSymbolicInput("0", varsUsed);
        matrix[3][2] = parseSymbolicInput("-9", varsUsed);
        matrix[3][3] = parseSymbolicInput("10*(x_l-3) - 9*(x_l-2)", varsUsed);
        matrix[3][4] = parseSymbolicInput("-1*(t_l-3)*((x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2))", varsUsed);

        matrix[4][0] = parseSymbolicInput("0", varsUsed);
        matrix[4][1] = parseSymbolicInput("0", varsUsed);
        matrix[4][2] = parseSymbolicInput("0", varsUsed);
        matrix[4][3] = parseSymbolicInput("-10", varsUsed);
        matrix[4][4] = parseSymbolicInput("(x_l-2)*(x_l-1)*x_l*(x_l+1)+(((5*(x_l+1))+(3*(x_l-3)))*((x_l-2)+x_l)+(6*(x_l-1)*x_l)-2*(x_l-3)*(x_l-2)+2*d)*(d/10)", varsUsed);

        return matrix;
    }
}
