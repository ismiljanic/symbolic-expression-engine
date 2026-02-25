package symbolicdet.utils;

import java.io.IOException;

public class PythonRunner {

    private static final String PYTHON = System.getProperty("os.name")
            .toLowerCase().contains("win") ? "python" : "python3";

    /**
     * Executes a Python script file with a single string argument.
     *
     * @param scriptPath absolute or relative path to the .py file
     * @param argument   the argument passed as sys.argv[1] (will be shell-quoted)
     * @return stdout of the script, trimmed
     */
    public static String run(String scriptPath, String argument) {
        try {
            Process process = new ProcessBuilder(PYTHON, scriptPath, argument)
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