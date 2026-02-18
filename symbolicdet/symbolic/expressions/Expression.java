package symbolicdet.symbolic.expressions;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

public class Expression extends Symbol {
    private Symbol left, right;
    private char operator;

    public Expression(Symbol left, char op, Symbol right) {
        this.left = left;
        this.operator = op;
        this.right = right;
    }

    @Override
    public String toExpr() {
        String leftExpr = left.toExpr();
        String rightExpr = right.toExpr();

        // Always ensure proper spacing between operands
        if (operator == '*') { // Guard against accidental merges like "(l+1)1.000000"
            if (!leftExpr.endsWith(" ") && !leftExpr.endsWith("*")) leftExpr += " ";
            if (!rightExpr.startsWith(" ")) rightExpr = " " + rightExpr;
            return "(" + leftExpr + "*" + rightExpr + ")";
        }
        return "(" + leftExpr + " " + operator + " " + rightExpr + ")";
    }

    @Override
    public double eval(Map<String, Double> vars) {
        double leftOperand = left.eval(vars);
        double rightOperand = right.eval(vars);
        return switch (operator) {
            case '+' -> leftOperand + rightOperand;
            case '-' -> leftOperand - rightOperand;
            case '*' -> leftOperand * rightOperand;
            case '/' -> leftOperand / rightOperand;
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    @Override
    public BigDecimal evalBD(Map<String, Double> vars) {
        BigDecimal leftOperand = left.evalBD(vars);
        BigDecimal rightOperand = right.evalBD(vars);

        return switch (operator) {
            case '+' -> leftOperand.add(rightOperand);
            case '-' -> leftOperand.subtract(rightOperand);
            case '*' -> leftOperand.multiply(rightOperand);
            case '/' -> leftOperand.divide(rightOperand, MathContext.DECIMAL128);
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    public Symbol getLeft() {
        return left;
    }

    public void setLeft(Symbol left) {
        this.left = left;
    }

    public Symbol getRight() {
        return right;
    }

    public void setRight(Symbol right) {
        this.right = right;
    }

    public char getOperator() {
        return operator;
    }

    public void setOperator(char operator) {
        this.operator = operator;
    }
}