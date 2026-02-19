package symbolicdet.symbolic.poly;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public final class PolynomialOps {

    private PolynomialOps() {
    }

    public static List<PolyTerm> negate(List<PolyTerm> list) {
        List<PolyTerm> r = new ArrayList<>();
        for (PolyTerm t : list)
            r.add(new PolyTerm(t.coeff.negate(), t.xx, t.lx, t.dx, t.lambdax));
        return r;
    }

    public static List<PolyTerm> add(List<PolyTerm> a, List<PolyTerm> b) {
        List<PolyTerm> out = new ArrayList<>(a);
        out.addAll(b);
        return combine(out);
    }

    public static List<PolyTerm> multiply(List<PolyTerm> a, List<PolyTerm> b) {
        List<PolyTerm> res = new ArrayList<>();
        for (PolyTerm A : a)
            for (PolyTerm B : b)
                res.add(new PolyTerm(
                        A.coeff.multiply(B.coeff, MathContext.DECIMAL128),
                        A.xx + B.xx, A.lx + B.lx, A.dx + B.dx, A.lambdax + B.lambdax));
        return combine(res);
    }

    public static List<PolyTerm> combine(List<PolyTerm> list) {
        Map<String, BigDecimal> map = new HashMap<>();

        for (PolyTerm t : list) {
            String key = t.xx + "," + t.lx + "," + t.dx + "," + t.lambdax;
            map.put(key, map.getOrDefault(key, BigDecimal.ZERO)
                    .add(t.coeff, MathContext.DECIMAL128));
        }

        List<PolyTerm> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            if (e.getValue().abs().compareTo(new BigDecimal("1e-4")) < 0)
                continue; // treat as zero

            String[] p = e.getKey().split(",");
            int xx = Integer.parseInt(p[0]);
            int lx = Integer.parseInt(p[1]);
            int dx = Integer.parseInt(p[2]);
            int lambdax = Integer.parseInt(p[3]);

            out.add(new PolyTerm(e.getValue(), xx, lx, dx, lambdax));
        }

        return out;
    }
}
