package symbolicdet.utils;

public class SymbolicTracer {
    private int depth = 0;

    public void log(String msg) {
        System.out.println("  ".repeat(depth) + msg);
    }

    public void indent() {
        depth++;
    }

    public void unindent() {
        if (depth > 0) depth--;
    }
}