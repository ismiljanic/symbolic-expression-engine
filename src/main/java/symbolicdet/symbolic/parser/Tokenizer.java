package symbolicdet.symbolic.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    public static List<String> tokenize(String s, StringBuilder log) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+\\.\\d+|\\d+|[A-Za-z_][A-Za-z0-9_]*|[+\\-*/()^]").matcher(s);
        while (m.find()) tokens.add(m.group());
        return tokens;
    }
}
