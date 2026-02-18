# Symbolic Determinant Engine for Parameterized Tridiagonal Matrices

A Java implementation of a **computer algebra system (CAS)** for symbolic determinant computation of parameterized tridiagonal-like matrices. The system constructs exact algebraic expressions, performs symbolic expansion, simplifies results to a closed-form polynomial and verifies them against a high-precision numeric LU decomposition, all within a purpose-built symbolic algebra layer.

---

## Mathematical Background

Given parameters:

- `x < 0`
- `0 < d < x^2`
- `l ∈ Z (integer)`

The sequences are defined as:

`x_l = x - 2 * (l - 1) * l`

`t_l = x_l * x_{l+1} - d`

All matrix entries are symbolic expressions in `x_l`, `t_l` and integer coefficients derived from this parameterization. The matrix size n is arbitrary.

---

## Features

### Symbolic Matrix Construction
- Builds an `nxn` symbolic matrix of arbitrary size from the recurrence relations above.
- Matrix entries are algebraic expressions involving `x_l`, `t_l` and integer coefficients.
- Predefined test matrices are available for standard sizes (up to n = 5).
- Displays the matrix in a labeled tabular format, truncating long expressions for readability.

### Symbolic Determinant — Cofactor Expansion
- Computes the determinant symbolically via recursive **(cofactor) expansion**.
- Outputs a step-by-step trace at each recursion level: the pivot element selected, its matrix position and the resulting minor matrix displayed in full.
- Produces the complete unevaluated **symbolic expression tree** of the determinant.
- Expands and collects terms to yield a **simplified closed-form polynomial** in `x` and `d` with rational coefficients.

### Numeric Verification — Symbolic Substitution
- Accepts user-supplied values for `x`, `l` and `d`.
- Substitutes these values directly into the symbolic determinant expression.
- Outputs the resulting numeric value.

### Numeric Verification — LU Decomposition
- Constructs the numeric matrix by substituting parameter values into each symbolic entry.
- Performs **LU decomposition with partial (column) pivoting** using high-precision arithmetic.
- Displays at each step: the pivot selection, the running partial determinant and each elimination factor and row operation applied.
- The final determinant is the product of all diagonal pivots, serving as an independent check against the symbolic result.

### Reduced Matrix (Minor) Determinant
- Prompts the user to optionally compute the determinant of a submatrix.
- Accepts a row index `i` and column index `j` (0-based) to remove.
- Constructs and computes the determinant of the resulting `(n-1)x(n-1)` minor through the same pipeline.

---

## Project Structure

```
.
├── docs
    ├── TASK.md/
├── symbolicdet
    ├── cas/
    ├── cli/
    │   ├── MatrixPrompt.java
    │   └── NumericInputs.java
    │   └── YesNoPrompt.java
    ├── matrix/
    │   ├── DeterminantService.java
    │   ├── Matrix.java
    │   ├── PredefinedMatrices.java
    │   ├── SubstituteNumericMatrix.java
    ├── symbolic/
    │   └── eval/
    │       ├── CalculateSymbolicLUDeterminant.java
    │       ├── PolynomialNumericEvaluator.java
    │   └── expressions/
    │       ├── Constant.java
    │       ├── Expression.java
    │       ├── Symbol.java
    │       ├── Variable.java
    │   └── parser/
    │       ├── ExpressionParser.java
    │       ├── InfixPostfixConverter.java
    │       ├── ParserSymbolicInput.java
    │       ├── PostfixPolyNomialEvaluator.java
    │       ├── Preprocessor.java
    │       ├── Tokenizer.java
    │   └── poly/
    │       ├── PolynomialFormatter.java
    │       ├── PolynomialOps.java
    │       ├── PolyTerm.java
    │   └── simplify/
    │       ├── PolySimplifier.java
    │       ├── SimplifySymbolic.java
    └── utils/
    │       ├── CheckIfNumeric.java
    │       ├── ComputeOffset.java
    │       ├── ExpandSymbolic.java
    │       ├── ExtractVariablesFromMatrix.java
    │       ├── PopulateRemainingVariables.java
    │       ├── PopulateTLVaribales.java
    │       ├── PopulateXLVaribales.java
    │       ├── PrintNumericMatrix.java
    │       ├── PrintSymbolicMatrix.java
    │       ├── SymbolicTracer.java
    │       ├── TraceNumericDeterminant.java
    ├── Main.java
    ├── .gitignore
    ├── LICENSE
    ├── README.md
```

---

## Usage

Build and run from the project root:

```bash
javac -d out $(find . -name "*.java")
java -cp out Main
```

You will be guided through the following interactive steps:

```
Enter matrix size (n): 5
```

The system constructs the symbolic matrix and displays it in tabular form.

```
Calculate numeric determinant with variables? (yes/no): yes
Enter value for x: 5
Enter value for l: 4
Enter value for d: 12
```

```
Run numeric LU with these values for correct pivoting? (yes/no): yes
```

```
Compute determinant of reduced matrix? (yes/no): yes
Enter row index to remove (0-based): 3
Enter column index to remove (0-based): 3
```

---

## Example Output (n = 5, x = 5, l = 4, d = 12)

**Symbolic determinant — final polynomial:**

```
-5308416*x^3*d + 1393459.2*x^2*d^2 + 69009408*x^2*d - 95846.4*x*d^3
- 14133657.6*x*d^2 - 250822656*x*d + 1843.2*d^4 + 549273.6*d^3
+ 31336243.2*d^2 + 238878720*d
```

**Numeric result — both methods must agree:**

```
Numeric determinant (symbolic substitution): 69274828.800000
Numeric determinant (LU-based):              69274828.800000
```

---

## Requirements

- Java 17+
- No external dependencies — the CAS layer and numeric engine are implemented from scratch.

---

## Notes

- The symbolic expression tree preserves the full unevaluated structure throughout expansion; the polynomial form is produced only at the final collection step.
- Numeric LU uses partial pivoting and high-precision arithmetic to prevent floating-point cancellation in ill-conditioned matrices.
- Both determinant paths must agree to at least 6 decimal places; any discrepancy indicates an error in the symbolic simplification stage.
- This project intentionally avoids third-party CAS libraries — the symbolic algebra layer is purpose-built for this parameterization as the primary thesis contribution.
