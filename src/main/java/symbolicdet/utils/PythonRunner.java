package symbolicdet.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonRunner {

    private static final String PYTHON = System.getProperty("os.name")
            .toLowerCase().contains("win") ? "python" : "python3";

    /**
     * Executes a Python script with a single string argument.
     */
    public static String run(String scriptPath, String argument) {
        return run(scriptPath, argument, new String[0]);
    }

    /**
     * Executes a Python script with a primary argument plus any number of
     * additional arguments (e.g. the outer variable choice for factorization).
     */
    public static String run(String scriptPath, String argument, String... extraArgs) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(PYTHON);
            cmd.add(scriptPath);
            cmd.add(argument);
            cmd.addAll(Arrays.asList(extraArgs));

            Process process = new ProcessBuilder(cmd)
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
}