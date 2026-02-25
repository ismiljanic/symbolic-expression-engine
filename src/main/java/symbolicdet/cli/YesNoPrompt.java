package symbolicdet.cli;

import java.util.Scanner;

public class YesNoPrompt {
    public static boolean askYesNo(Scanner sc, String prompt) {
        String input;
        while (true) {
            System.out.print(prompt + " (yes/no): ");
            input = sc.nextLine().trim().toLowerCase();
            if (input.isEmpty()) continue;
            if (input.equals("yes")) return true;
            if (input.equals("no")) return false;
            System.out.println("Invalid input. Please type 'yes' or 'no'.");
        }
    }
}
