package dev.dialogguard;

import net.minecraft.dialog.type.Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Decides whether a server-supplied {@link Dialog} is dangerous.
 *
 * Two independent signals:
 *   1. Structural: the dialog declares input controls (text boxes etc.).
 *   2. Textual: title/body/input labels read like a credential or verification-code request.
 */
public final class DialogInspector {
    private DialogInspector() {}

    public record Verdict(boolean block, String reason) {
        public static final Verdict ALLOW = new Verdict(false, "");
    }

    public static Verdict inspect(Dialog dialog) {
        GuardConfig cfg = GuardConfig.get();
        List<String> reasons = new ArrayList<>();

        // Signal 1: every dialog exposes shared data (title, body, inputs) via common().
        boolean hasInputs = !dialog.common().inputs().isEmpty();
        if (hasInputs && cfg.blockInputDialogs) {
            reasons.add("contains input fields");
        }

        // Signal 2: keyword scan over everything the player would read.
        if (cfg.blockKeywordDialogs) {
            List<String> hits = matchKeywords(collectText(dialog), cfg);
            if (!hits.isEmpty()) {
                reasons.add("asks for sensitive data: " + String.join(", ", hits));
            }
        }

        return reasons.isEmpty() ? Verdict.ALLOW : new Verdict(true, String.join("; ", reasons));
    }

    /** Flatten title + body + input definitions into one lower-case string. */
    private static String collectText(Dialog dialog) {
        var common = dialog.common();
        StringBuilder sb = new StringBuilder();
        sb.append(common.title().getString());
        common.externalTitle().ifPresent(t -> sb.append(' ').append(t.getString()));
        // Records print their nested Text contents (literal / translatable), which is enough
        // for keyword matching and avoids depending on every DialogBody / input subtype.
        sb.append(' ').append(common.body());
        sb.append(' ').append(common.inputs());
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static List<String> matchKeywords(String text, GuardConfig cfg) {
        List<String> strong = new ArrayList<>();
        for (String k : cfg.strongKeywords) {
            if (containsTerm(text, k.toLowerCase(Locale.ROOT))) strong.add(k);
        }
        if (!strong.isEmpty()) return strong;

        List<String> weak = new ArrayList<>();
        for (String k : cfg.weakKeywords) {
            if (containsTerm(text, k.toLowerCase(Locale.ROOT))) weak.add(k);
        }
        return weak.size() >= 2 ? weak : List.of();
    }

    /** Matches the term at the start of a word ("auth" matches "authenticate", "oauth" does not match "auth"). */
    private static boolean containsTerm(String text, String term) {
        if (term.isEmpty()) return false;
        int from = 0;
        int idx;
        while ((idx = text.indexOf(term, from)) >= 0) {
            if (idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1))) return true;
            from = idx + 1;
        }
        return false;
    }
}
