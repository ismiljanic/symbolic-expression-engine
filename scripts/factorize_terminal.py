"""
Terminal factorization script.
Usage: python3 factorize_terminal.py "<sympy_expr>"
"""
import sys
from sympy import *

x, l, d, lam = symbols('x l d lam')
expr = eval(sys.argv[1])


def fmt_power(var, exp):
    e = int(exp.as_base_exp()[1]) if exp != 1 else 0
    if e == 0:
        return ""
    if e == 1:
        return str(var)
    return f"{var}^{e}"


def sympy_to_display(s):
    """Convert SymPy's string output to display notation."""
    return (s
            .replace("lam", "λ")
            .replace("**", "^")
            .replace("*", "·"))


def fmt_coeff(c):
    c = factor(c)
    s = sympy_to_display(str(c))
    if s == "1":
        return ""
    if s == "-1":
        return "-1"
    return f"({s})" if (c.is_Add or (c.is_Mul and any(a.is_Add for a in c.args))) else s


collected = collect(expr, lam, evaluate=False)
lam_powers = sorted(collected.keys(), key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)

lines = []
for lam_pow in lam_powers:
    coeff = collected[lam_pow]
    lam_str = fmt_power("λ", lam_pow)
    inner = collect(coeff, x, evaluate=False)
    x_powers = sorted(inner.keys(), key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)
    for x_pow in x_powers:
        c = fmt_coeff(inner[x_pow])
        x_str = fmt_power("x", x_pow)
        parts = [p for p in [c, x_str, lam_str] if p]
        lines.append(" · ".join(parts) if parts else "1")

print("det =")
print("  " + lines[0])
for line in lines[1:]:
    print("  + " + line)