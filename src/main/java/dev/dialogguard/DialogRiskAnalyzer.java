package dev.dialogguard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure policy engine for deciding whether dialog content looks unsafe.
 * Kept independent of Minecraft UI/network classes so the policy can be covered by GameTests.
 */
public final class DialogRiskAnalyzer {
    private DialogRiskAnalyzer() {}

    public record Verdict(boolean block, String reason) {
        public static final Verdict ALLOW = new Verdict(false, "");
    }

    public static Verdict inspect(
            boolean hasInputs,
            String text,
            boolean blockInputDialogs,
            boolean blockKeywordDialogs,
            List<String> strongKeywords,
            List<String> weakKeywords) {
        List<String> reasons = new ArrayList<>();

        if (hasInputs && blockInputDialogs) {
            reasons.add("contains input fields");
        }

        if (blockKeywordDialogs) {
            List<String> hits = matchKeywords(text == null ? "" : text.toLowerCase(Locale.ROOT),
                    strongKeywords, weakKeywords);
            if (!hits.isEmpty()) {
                reasons.add("asks for sensitive data: " + String.join(", ", hits));
            }
        }

        return reasons.isEmpty() ? Verdict.ALLOW : new Verdict(true, String.join("; ", reasons));
    }

    private static List<String> matchKeywords(String text, List<String> strongKeywords, List<String> weakKeywords) {
        List<String> strong = matchingTerms(text, strongKeywords);
        if (!strong.isEmpty()) return strong;

        List<String> weak = matchingTerms(text, weakKeywords);
        return weak.size() >= 2 ? weak : List.of();
    }

    /**
     * Matches a keyword only at the beginning of a word and ignores duplicate config entries.
     * Returning unique hits ensures a duplicated weak keyword cannot satisfy the two-signal rule.
     */
    private static List<String> matchingTerms(String text, List<String> terms) {
        if (terms == null || terms.isEmpty()) return List.of();

        Set<String> seen = new LinkedHashSet<>();
        List<String> matches = new ArrayList<>();
        for (String configuredTerm : terms) {
            if (configuredTerm == null) continue;

            String term = configuredTerm.trim().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || !seen.add(term)) continue;
            if (containsTerm(text, term)) matches.add(configuredTerm.trim());
        }
        return matches;
    }

    /** Matches at a word start: "auth" matches "authenticate", but not "oauth". */
    private static boolean containsTerm(String text, String term) {
        int from = 0;
        int index;
        while ((index = text.indexOf(term, from)) >= 0) {
            if (index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1))) return true;
            from = index + 1;
        }
        return false;
    }
}
