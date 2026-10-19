package dev.dialogguard;

import net.minecraft.dialog.type.Dialog;

/** Adapts a Minecraft dialog to the policy-only {@link DialogRiskAnalyzer}. */
public final class DialogInspector {
    private DialogInspector() {}

    public static DialogRiskAnalyzer.Verdict inspect(Dialog dialog) {
        GuardConfig cfg = GuardConfig.get();
        var common = dialog.common();
        StringBuilder text = new StringBuilder();

        text.append(common.title().getString());
        common.externalTitle().ifPresent(title -> text.append(' ').append(title.getString()));
        // Include body and input definitions without depending on every DialogBody/input subtype.
        text.append(' ').append(common.body());
        text.append(' ').append(common.inputs());

        return DialogRiskAnalyzer.inspect(
                !common.inputs().isEmpty(),
                text.toString(),
                cfg.blockInputDialogs,
                cfg.blockKeywordDialogs,
                cfg.strongKeywords,
                cfg.weakKeywords);
    }
}
