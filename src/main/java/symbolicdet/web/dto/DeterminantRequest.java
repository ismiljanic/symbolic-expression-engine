package symbolicdet.web.dto;

public class DeterminantRequest {

    private String[][] matrix;

    private Double xValue;
    private Double lValue;
    private Double dValue;
    private Double lambdaValue;

    private boolean factorize = false;
    private boolean detailedLU = false;

    private String factorizeVar = "d";

    private boolean useTridiagonal = false;
    private Integer tridiagonalSize;

    public String[][] getMatrix() { return matrix; }
    public void setMatrix(String[][] matrix) { this.matrix = matrix; }

    public Double getXValue() { return xValue; }
    public void setXValue(Double xValue) { this.xValue = xValue; }

    public Double getLValue() { return lValue; }
    public void setLValue(Double lValue) { this.lValue = lValue; }

    public Double getDValue() { return dValue; }
    public void setDValue(Double dValue) { this.dValue = dValue; }

    public Double getLambdaValue() { return lambdaValue; }
    public void setLambdaValue(Double lambdaValue) { this.lambdaValue = lambdaValue; }

    public boolean isFactorize() { return factorize; }
    public void setFactorize(boolean factorize) { this.factorize = factorize; }

    public boolean isDetailedLU() { return detailedLU; }
    public void setDetailedLU(boolean detailedLU) { this.detailedLU = detailedLU; }

    public String getFactorizeVar() { return factorizeVar != null ? factorizeVar : "d"; }
    public void setFactorizeVar(String factorizeVar) { this.factorizeVar = factorizeVar; }

    public boolean isUseTridiagonal() { return useTridiagonal; }
    public void setUseTridiagonal(boolean useTridiagonal) { this.useTridiagonal = useTridiagonal; }

    public Integer getTridiagonalSize() { return tridiagonalSize; }
    public void setTridiagonalSize(Integer tridiagonalSize) { this.tridiagonalSize = tridiagonalSize; }

    public boolean hasNumericInputs() {
        return xValue != null && lValue != null;
    }
}