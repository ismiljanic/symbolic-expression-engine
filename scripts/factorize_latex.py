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
        raw = inner[x_pow]
        factored = factor(raw)

        raw_terms = Add.make_args(raw)

        terms.append({
            "factored": clean_latex(latex(factored * x_pow * lam_pow)),
            "from": [clean_latex(latex(t)) for t in raw_terms]
        })
    groups.append({"lam_exp": lam_exp, "terms": terms})

print(json.dumps(groups))