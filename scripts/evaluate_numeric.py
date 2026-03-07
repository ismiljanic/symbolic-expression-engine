"""
Numeric evaluator script.
Usage: python3 evaluate_numeric.py "<sympy_expr>" <x> <l> <d> <lam>

Evaluates the polynomial at given values using Python's Decimal for
arbitrary precision (equivalent to Java BigDecimal).
"""
import sys
from decimal import Decimal, getcontext

getcontext().prec = 200

def main():
    if len(sys.argv) < 6:
        print("Usage: evaluate_numeric.py <expr> <x> <l> <d> <lam>", file=sys.stderr)
        sys.exit(1)

    raw_expr = sys.argv[1]
    x_val    = Decimal(sys.argv[2])
    l_val    = Decimal(sys.argv[3])
    d_val    = Decimal(sys.argv[4])
    lam_val  = Decimal(sys.argv[5])

    # Replace variable names with Decimal values
    # We do this by defining them in a local namespace
    ns = {
        "x":   x_val,
        "l":   l_val,
        "d":   d_val,
        "lam": lam_val,
        "Decimal": Decimal,
    }

    # Convert ** exponents to work with Decimal
    expr = raw_expr.replace("^", "**")

    try:
        result = eval(expr, {"__builtins__": {}}, ns)
        print(result)
    except Exception as e:
        print(f"Error evaluating expression: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()