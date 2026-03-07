"""
Terminal factorization script.
Usage: python3 factorize_terminal.py "<sympy_expr>" <outer_var>
  outer_var: one of x, l, d, lam/lambda  (default: d)

Grouping: outer_var desc -> middle desc -> inner desc -> remainder factored over l.
Uses FLINT for fast factorization if available, falls back to SymPy.
"""
import sys, io
from sympy import *

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')  # ensures λ prints

# define symbols first
x, l, d, lam = symbols('x l d lam')
symbol_dict = {'x': x, 'l': l, 'd': d, 'lam': lam, 'lambda': lam}

# Read expression from file if first argument is a .txt file
if len(sys.argv) > 1 and sys.argv[1].endswith(".txt"):
    with open(sys.argv[1]) as f:
        expr_str = f.read()
else:
    expr_str = sys.argv[1]

# Evaluate using the symbol dictionary
expr = eval(expr_str, symbol_dict)

# outer variable
outer_key = sys.argv[2].strip().lower() if len(sys.argv) > 2 else "d"

ALL_SYMS        = {"x": x, "l": l, "d": d, "lam": lam, "lambda": lam}
SYM_NAMES       = {x: "x", l: "l", d: "d", lam: "λ"}
PREFERRED_ORDER = [d, x, lam, l]  # l is always the innermost remainder


# ---------------------------------------------------------------------------
# FLINT fast-factor setup
# ---------------------------------------------------------------------------

def _try_import_flint():
    try:
        from flint import fmpz_poly
        return fmpz_poly
    except ImportError:
        return None

FLINT_POLY = _try_import_flint()


def flint_poly_to_sympy(fp, var):
    coeffs = list(fp)  # low -> high order
    return sum(int(c) * var**i for i, c in enumerate(coeffs))


def fast_factor_in_l(expr):
    """
    Factor expr as a polynomial in l.
    - Pure-l poly (no x/d/lam): uses FLINT (milliseconds).
    - Mixed poly: uses SymPy with domain hint (faster than bare factor()).
    - Falls back to bare SymPy factor() on any error.
    """
    if FLINT_POLY is not None:
        try:
            free = expr.free_symbols
            if not (free - {l}):
                # Pure univariate in l — use FLINT
                p_sympy = Poly(expr, l, domain='ZZ')
                coeffs  = [int(c) for c in p_sympy.all_coeffs()[::-1]]
                fp      = FLINT_POLY(coeffs)
                content, factors = fp.factor()
                result = Integer(int(content))
                for factor_poly, exp in factors:
                    result *= flint_poly_to_sympy(factor_poly, l) ** exp
                return result
        except Exception:
            pass

    # Multivariate or FLINT unavailable — SymPy with domain hint
    try:
        p = Poly(expr, l, domain='ZZ[x,d,lam]')
        return factor(p.as_expr(), l)
    except Exception:
        pass

    return factor(expr)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def fmt_power(var_name, exp):
    e = int(exp.as_base_exp()[1]) if exp != 1 else 0
    if e == 0:
        return ""
    if e == 1:
        return var_name
    return f"{var_name}^{e}"


def sympy_to_display(s):
    return (s
            .replace("lam", "λ")
            .replace("**", "^")
            .replace("*", "·"))


def fmt_remainder(c):
    """Factor c (a poly in l) and format it nicely."""
    c_factored = fast_factor_in_l(c)
    s = sympy_to_display(str(c_factored))
    if s == "1":
        return ""
    if s == "-1":
        return "-1"
    if c_factored.is_Mul:
        args     = c_factored.args
        int_part = [a for a in args if a.is_Integer]
        rest     = [a for a in args if not a.is_Integer]
        if int_part and rest:
            coeff_str = sympy_to_display(str(int_part[0]))
            rest_expr = Mul(*rest)
            rest_str  = sympy_to_display(str(rest_expr))
            if rest_expr.is_Add or (rest_expr.is_Mul and any(a.is_Add for a in rest_expr.args)):
                return f"({coeff_str}·({rest_str}))"
            return f"({coeff_str}·{rest_str})"
    if c_factored.is_Add:
        return f"({s})"
    return s


def sort_keys(keys):
    return sorted(keys, key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

expr = eval(sys.argv[1])

outer_key = sys.argv[2].strip().lower() if len(sys.argv) > 2 else "d"
if outer_key not in ALL_SYMS:
    print(f"Unknown variable '{outer_key}'. Choose from: x, l, d, lam")
    sys.exit(1)

outer_sym = ALL_SYMS[outer_key]

# Build collect levels: outer -> middle -> inner, l always stays as remainder
collect_vars = [v for v in PREFERRED_ORDER if v != outer_sym and v != l]
# collect_vars[0] = middle, collect_vars[1] = inner

outer_name = SYM_NAMES[outer_sym]
mid_name   = SYM_NAMES[collect_vars[0]]
inner_name = SYM_NAMES[collect_vars[1]]

# ---------------------------------------------------------------------------
# Step 1: collect full structure, gather all remainders
# ---------------------------------------------------------------------------

collected_outer = collect(expr, outer_sym, evaluate=False)
outer_powers    = sort_keys(collected_outer.keys())

remainders_to_factor = {}  # (outer_pow, mid_pow, inner_pow) -> raw expr
structure            = []  # ordered for output

for outer_pow in outer_powers:
    coeff_outer  = collected_outer[outer_pow]
    collected_mid = collect(coeff_outer, collect_vars[0], evaluate=False)
    mid_powers    = sort_keys(collected_mid.keys())

    mid_list = []
    for mid_pow in mid_powers:
        coeff_mid       = collected_mid[mid_pow]
        collected_inner = collect(coeff_mid, collect_vars[1], evaluate=False)
        inner_powers    = sort_keys(collected_inner.keys())

        inner_list = []
        for inner_pow in inner_powers:
            remainder = collected_inner[inner_pow]
            key       = (outer_pow, mid_pow, inner_pow)
            remainders_to_factor[key] = remainder
            inner_list.append((inner_pow, key))

        mid_list.append((mid_pow, inner_list))
    structure.append((outer_pow, mid_list))

# ---------------------------------------------------------------------------
# Step 2: factor all remainders in one pass
# ---------------------------------------------------------------------------

factored_cache = {}
for key, raw in remainders_to_factor.items():
    factored_cache[key] = fast_factor_in_l(raw)

# ---------------------------------------------------------------------------
# Step 3: build output from cache
# ---------------------------------------------------------------------------

lines = []
for outer_pow, mid_list in structure:
    outer_str = fmt_power(outer_name, outer_pow)
    for mid_pow, inner_list in mid_list:
        mid_str = fmt_power(mid_name, mid_pow)
        for inner_pow, key in inner_list:
            inner_str  = fmt_power(inner_name, inner_pow)
            c_factored = factored_cache[key]

            # Format using the already-factored result
            s = sympy_to_display(str(c_factored))
            if s == "1":
                rem_str = ""
            elif s == "-1":
                rem_str = "-1"
            elif c_factored.is_Mul:
                args     = c_factored.args
                int_part = [a for a in args if a.is_Integer]
                rest     = [a for a in args if not a.is_Integer]
                if int_part and rest:
                    coeff_str = sympy_to_display(str(int_part[0]))
                    rest_expr = Mul(*rest)
                    rest_str  = sympy_to_display(str(rest_expr))
                    if rest_expr.is_Add or (rest_expr.is_Mul and any(a.is_Add for a in rest_expr.args)):
                        rem_str = f"({coeff_str}·({rest_str}))"
                    else:
                        rem_str = f"({coeff_str}·{rest_str})"
                else:
                    rem_str = f"({s})" if c_factored.is_Add else s
            elif c_factored.is_Add:
                rem_str = f"({s})"
            else:
                rem_str = s

            parts = [p for p in [rem_str, outer_str, mid_str, inner_str] if p]
            lines.append(" · ".join(parts) if parts else "1")

print("det =")
if lines:
    print("  " + lines[0])
    for line in lines[1:]:
        print("  + " + line)