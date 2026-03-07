"""
LaTeX/JSON factorization script.
Usage: python3 factorize_latex.py "<sympy_expr>"
Outputs JSON grouped by d -> x -> lam powers, with each remainder factored over l.
Uses FLINT for fast factorization if available, falls back to SymPy.
"""
import sys
import json
from sympy import *

x, l, d, lam = symbols('x l d lam')


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

def clean_latex(s):
    s = s.replace(r'\lambda', '__LAMBDA__')
    s = s.replace('lam', r'\lambda')
    s = s.replace('__LAMBDA__', r'\lambda')
    s = s.replace(r'\left(', '(').replace(r'\right)', ')')
    s = s.replace(r'\left[', '[').replace(r'\right]', ']')
    s = s.replace(r'\left{', '{').replace(r'\right}', '}')
    s = s.replace(r'\left|', '|').replace(r'\right|', '|')
    s = s.replace(r'\cdot', r'\,')
    return s


def sort_keys(keys):
    return sorted(keys, key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

expr = eval(sys.argv[1])

# ---------------------------------------------------------------------------
# Step 1: collect full structure, gather all remainders
# ---------------------------------------------------------------------------

collected_d = collect(expr, d, evaluate=False)
d_powers    = sort_keys(collected_d.keys())

remainders_to_factor = {}  # (d_pow, x_pow, lam_pow) -> raw expr
structure            = []  # ordered for output

for d_pow in d_powers:
    coeff_d  = collected_d[d_pow]
    inner_x  = collect(coeff_d, x, evaluate=False)
    x_powers = sort_keys(inner_x.keys())

    x_list = []
    for x_pow in x_powers:
        coeff_x   = inner_x[x_pow]
        inner_lam = collect(coeff_x, lam, evaluate=False)
        lam_powers = sort_keys(inner_lam.keys())

        lam_list = []
        for lam_pow in lam_powers:
            raw = inner_lam[lam_pow]
            key = (d_pow, x_pow, lam_pow)
            remainders_to_factor[key] = raw
            lam_list.append((lam_pow, key))

        x_list.append((x_pow, lam_list))
    structure.append((d_pow, x_list))

# ---------------------------------------------------------------------------
# Step 2: factor all remainders in one pass
# ---------------------------------------------------------------------------

factored_cache = {}
for key, raw in remainders_to_factor.items():
    factored_cache[key] = fast_factor_in_l(raw)

# ---------------------------------------------------------------------------
# Step 3: build JSON output from cache
# ---------------------------------------------------------------------------

groups = []
for d_pow, x_list in structure:
    d_exp    = int(d_pow.as_base_exp()[1]) if d_pow != 1 else 0
    x_groups = []

    for x_pow, lam_list in x_list:
        x_exp     = int(x_pow.as_base_exp()[1]) if x_pow != 1 else 0
        lam_terms = []

        for lam_pow, key in lam_list:
            lam_exp   = int(lam_pow.as_base_exp()[1]) if lam_pow != 1 else 0
            raw       = remainders_to_factor[key]
            factored  = factored_cache[key]

            raw_terms = Add.make_args(raw)
            lam_terms.append({
                "lam_exp":  lam_exp,
                "factored": clean_latex(latex(factored * lam_pow)),
                "from":     [clean_latex(latex(t)) for t in raw_terms]
            })

        x_groups.append({"x_exp": x_exp, "lam_terms": lam_terms})

    groups.append({"d_exp": d_exp, "x_groups": x_groups})

print(json.dumps(groups))