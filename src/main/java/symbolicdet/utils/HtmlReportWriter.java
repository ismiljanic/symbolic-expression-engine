package symbolicdet.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds and saves the HTML determinant report.
 *
 * <p>The HTML structure, CSS, and JS are plain files in the {@code resources/} folder
 * next to your project root:
 * <ul>
 *   <li>{@code resources/report.html} — page structure with {@code {{PLACEHOLDER}}} tokens
 *   <li>{@code resources/report.css}  — all styles, inlined into the {@code <style>} tag
 *   <li>{@code resources/report.js}   — all scripts, inlined into the {@code <script>} tag
 * </ul>
 */
public class HtmlReportWriter {

    private static final String RES = "resources/";

    public static void write(String terminalOutput, String latexJson, String originalPoly, String type, int rows, int cols) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String groupsHtml = buildGroupsHtml(latexJson);

        String html = loadResource("report.html")
                .replace("{{CSS}}", loadResource("report.css"))
                .replace("{{JS}}", loadResource("report.js"))
                .replace("{{TIMESTAMP}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .replace("{{ORIGINAL_POLY}}", ExpressionConverter.escapeHtml(originalPoly))
                .replace("{{GROUPS_HTML}}", groupsHtml)
                .replace("{{TERMINAL_OUTPUT}}", ExpressionConverter.escapeHtml(terminalOutput));

        try {
            Path reportDir = Path.of("report");
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
            }

            String filename = String.format("report_%s_%dx%d_%s.html", type, rows, cols, timestamp);
            Path filePath = reportDir.resolve(filename);

            Files.writeString(filePath, html);
            System.out.println("\nHTML report saved: " + filePath.toString());
        } catch (IOException e) {
            System.out.println("Could not write HTML report: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Resource loading
    // -------------------------------------------------------------------------

    private static String loadResource(String filename) {
        try {
            return Files.readString(Path.of(RES + filename));
        } catch (IOException e) {
            throw new IllegalStateException("Resource not found: " + RES + filename
                    + " — make sure the resources/ folder is in your working directory.", e);
        }
    }

    // -------------------------------------------------------------------------
    // JSON -> HTML groups
    // -------------------------------------------------------------------------

    private static String buildGroupsHtml(String latexJson) {
        StringBuilder groupsHtml = new StringBuilder();
        String[] groupBlocks = latexJson
                .replace("[{", "")
                .replace("}]", "")
                .split("\\}, \\{");

        for (String block : groupBlocks) {
            int lamExp = parseLamExp(block);
            List<String> terms = parseTerms(block);

            String lamLabel = lamExp == 0 ? "constant"
                    : lamExp == 1 ? "\\(\\lambda\\)"
                    : "\\(\\lambda^{" + lamExp + "}\\)";
            String groupId = "group_lam_" + lamExp;

            groupsHtml.append(String.format("""
                            <div class="lam-group">
                                <button class="group-header" onclick="toggle('%s')">
                                    <span class="group-label">Terms with %s</span>
                                    <span class="group-count">%d term%s</span>
                                    <span class="chevron" id="chevron_%s">&#9660;</span>
                                </button>
                                <div class="group-body" id="%s">
                                    <table class="terms">%s</table>
                                </div>
                            </div>
                            """,
                    groupId, lamLabel,
                    terms.size(), terms.size() == 1 ? "" : "s",
                    groupId, groupId,
                    buildTermRows(terms)));
        }
        return groupsHtml.toString();
    }

    private static String buildTermRows(List<String> terms) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            // Replace any 'lambda' with LaTeX \lambda
            String term = terms.get(i)
                    .replace("lambda", "λ")
                    .replace("lam", "λ");
            rows.append(String.format("""
                    <tr class="term-row">
                        <td class="sign">%s</td>
                        <td class="term">\\(%s\\)</td>
                    </tr>
                    """, i == 0 ? "" : "+", term));
        }
        return rows.toString();
    }

    // -------------------------------------------------------------------------
    // Minimal JSON parsing (avoids external dependency)
    // -------------------------------------------------------------------------

    private static int parseLamExp(String block) {
        Matcher m = Pattern.compile("\"lam_exp\": (-?\\d+)").matcher(block);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static List<String> parseTerms(String block) {
        List<String> terms = new ArrayList<>();
        Matcher arrayMatcher = Pattern.compile("\"terms\": \\[(.*)\\]$").matcher(block);
        if (!arrayMatcher.find()) return terms;
        Matcher termMatcher = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(arrayMatcher.group(1));
        while (termMatcher.find()) {
            terms.add(termMatcher.group(1));
        }
        return terms;
    }
}