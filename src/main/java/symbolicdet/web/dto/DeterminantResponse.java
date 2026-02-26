package symbolicdet.web.dto;

import java.util.List;

public class DeterminantResponse {

    private String symbolicDeterminant;
    private String expandedPolynomial;
    private List<FactorizedGroup> factorized;
    private Double numericFromSymbolic;
    private Double numericFromLU;
    private List<String> luSteps;
    private List<String> detectedVariables;
    private String error;

    private boolean needsXL;
    private boolean needsD;
    private boolean needsLambda;

    public static DeterminantResponse error(String message) {
        DeterminantResponse r = new DeterminantResponse();
        r.error = message;
        return r;
    }

    public String getSymbolicDeterminant() { return symbolicDeterminant; }
    public void setSymbolicDeterminant(String v) { this.symbolicDeterminant = v; }

    public String getExpandedPolynomial() { return expandedPolynomial; }
    public void setExpandedPolynomial(String v) { this.expandedPolynomial = v; }

    public List<FactorizedGroup> getFactorized() { return factorized; }
    public void setFactorized(List<FactorizedGroup> v) { this.factorized = v; }

    public Double getNumericFromSymbolic() { return numericFromSymbolic; }
    public void setNumericFromSymbolic(Double v) { this.numericFromSymbolic = v; }

    public Double getNumericFromLU() { return numericFromLU; }
    public void setNumericFromLU(Double v) { this.numericFromLU = v; }

    public List<String> getLuSteps() { return luSteps; }
    public void setLuSteps(List<String> v) { this.luSteps = v; }

    public List<String> getDetectedVariables() { return detectedVariables; }
    public void setDetectedVariables(List<String> v) { this.detectedVariables = v; }

    public String getError() { return error; }
    public void setError(String v) { this.error = v; }

    public boolean isNeedsXL() { return needsXL; }
    public void setNeedsXL(boolean v) { this.needsXL = v; }

    public boolean isNeedsD() { return needsD; }
    public void setNeedsD(boolean v) { this.needsD = v; }

    public boolean isNeedsLambda() { return needsLambda; }
    public void setNeedsLambda(boolean v) { this.needsLambda = v; }

    /**
     * Inner class to represent a group of factorized terms
     */
    public static class FactorizedGroup {
        private String power;
        private List<String> terms;

        public FactorizedGroup() {}

        public FactorizedGroup(String power, List<String> terms) {
            this.power = power;
            this.terms = terms;
        }

        public String getPower() { return power; }
        public void setPower(String v) { this.power = v; }

        public List<String> getTerms() { return terms; }
        public void setTerms(List<String> v) { this.terms = v; }
        @Override
        public String toString() {
            return "FactorizedGroup{factor='" + power + "', terms=" + terms + "}";
        }
    }
}