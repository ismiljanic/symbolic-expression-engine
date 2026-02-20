"""
LaTeX factorization script — outputs JSON for HTML report generation.
Usage: python3 factorize_latex.py "<sympy_expr>"
"""
import sys
import json
from sympy import *

x, l, d, lam = symbols('x l d lam')
expr = eval(sys.argv[1])

collected = collect(expr, lam, evaluate=False)
lam_powers = sorted(collected.keys(), key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)


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


groups = []
for lam_pow in lam_powers:
    coeff = collected[lam_pow]
    lam_exp = int(lam_pow.as_base_exp()[1]) if lam_pow != 1 else 0
    inner = collect(coeff, x, evaluate=False)
    x_powers = sorted(inner.keys(), key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)
    terms = []
    for x_pow in x_powers:
        c = factor(inner[x_pow])
        term_latex = clean_latex(latex(c * x_pow * lam_pow))
        terms.append(term_latex)
    groups.append({"lam_exp": lam_exp, "terms": terms})

print(json.dumps(groups))