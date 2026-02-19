package symbolicdet.utils;

import java.io.*;

public class FactorizeFinalExpression {

    public static String factor(String polynomial) {
        return toMathNotation(executeScript(buildScript(toSympyNotation(polynomial))));
    }

    private static String buildScript(String expr) {
        return """
                from sympy import *
                x, l, d, lam = symbols('x l d lam')
                expr = %s
                
                def fmt_power(var, exp):
                    e = int(exp.as_base_exp()[1]) if exp != 1 else 0
                    if e == 0: return ""
                    if e == 1: return str(var)
                    return f"{var}^{e}"
                
                def fmt_coeff(c):
                    c = factor(c)
                    s = str(c).replace("**", "^").replace("*", "·")
                    if s == "1": return ""
                    if s == "-1": return "-1"
                    return f"({s})" if (c.is_Add or (c.is_Mul and any(a.is_Add for a in c.args))) else s
                
                def print_collected(primary_var, primary_name, secondary_var, secondary_name):
                    collected = collect(expr, primary_var, evaluate=False)
                    primary_powers = sorted(collected.keys(),
                                       key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)
                    lines = []
                    for pri_pow in primary_powers:
                        coeff = collected[pri_pow]
                        pri_str = fmt_power(primary_name, pri_pow)
                        inner = collect(coeff, secondary_var, evaluate=False)
                        sec_powers = sorted(inner.keys(),
                                         key=lambda k: -int(k.as_base_exp()[1]) if k != 1 else 0)
                        for sec_pow in sec_powers:
                            c = fmt_coeff(inner[sec_pow])
                            sec_str = fmt_power(secondary_name, sec_pow)
                            parts = [p for p in [c, sec_str, pri_str] if p]
                            lines.append(" · ".join(parts) if parts else "1")
                    return lines
                
                lines = print_collected(lam, 'lam', x, 'x')
                print("det =")
                print("    " + lines[0])
                for line in lines[1:]:
                    print("  + " + line)
                """.formatted(expr);
    }

    private static String toMathNotation(String output) {
        return output
                .replace("lam", "λ")
                .replaceAll("\\*\\*", "^")
                .replaceAll("(?<=[0-9])\\*(?=[a-zA-Zλ])", "·")
                .replace("*", "·");
    }

    private static String executeScript(String script) {
        String python = System.getProperty("os.name")
                .toLowerCase().contains("win") ? "python" : "python3";
        try {
            Process process = new ProcessBuilder(python, "-c", script)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output.trim();
        } catch (IOException e) {
            return "Could not launch Python: " + e.getMessage()
                    + "\nMake sure Python3 and SymPy are installed.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted";
        }
    }

    private static String toSympyNotation(String expr) {
        return expr
                .replace("λ", "lam")
                .replace("lambda", "lam")
                .replaceAll("\\^(\\d+)", "**$1");
    }
}