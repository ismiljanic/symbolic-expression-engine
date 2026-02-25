package symbolicdet.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymbolicTracer {

    private final boolean printToStdout;
    private final List<String> log = new ArrayList<>();

    public SymbolicTracer() {
        this.printToStdout = true;
    }

    public SymbolicTracer(boolean printToStdout) {
        this.printToStdout = printToStdout;
    }

    public void trace(String message) {
        log.add(message);
        if (printToStdout) {
            System.out.println(message);
        }
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    public void reset() {
        log.clear();
    }

    public void log(String msg) {
        int depth = 0;
        System.out.println("  ".repeat(depth) + msg);
    }
}