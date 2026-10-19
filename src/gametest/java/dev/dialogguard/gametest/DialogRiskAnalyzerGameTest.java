package dev.dialogguard.gametest;

import dev.dialogguard.DialogRiskAnalyzer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;

import java.util.List;

/** Regression tests for the shared phishing classification policy. */
public final class DialogRiskAnalyzerGameTest {
    @GameTest
    public void blocksInputForms(TestContext context) {
        DialogRiskAnalyzer.Verdict verdict = DialogRiskAnalyzer.inspect(
                true, "A harmless survey", true, true,
                List.of("microsoft", "password"), List.of("login", "verification"));

        context.assertTrue(verdict.block(), Text.literal("Input-bearing dialogs must be blocked"));
        context.complete();
    }

    @GameTest
    public void blocksStrongCredentialTerms(TestContext context) {
        DialogRiskAnalyzer.Verdict verdict = DialogRiskAnalyzer.inspect(
                false, "Microsoft sign-in: enter your password", true, true,
                List.of("microsoft", "password"), List.of("login", "verification"));

        context.assertTrue(verdict.block(), Text.literal("A strong credential keyword must block"));
        context.complete();
    }

    @GameTest
    public void weakTermsNeedTwoDistinctMatches(TestContext context) {
        DialogRiskAnalyzer.Verdict oneWeakTerm = DialogRiskAnalyzer.inspect(
                false, "Enter the code", true, true,
                List.of("microsoft"), List.of("code", "verify"));
        DialogRiskAnalyzer.Verdict twoWeakTerms = DialogRiskAnalyzer.inspect(
                false, "Login verification required", true, true,
                List.of("microsoft"), List.of("login", "verification"));
        DialogRiskAnalyzer.Verdict duplicatedConfigTerm = DialogRiskAnalyzer.inspect(
                false, "Enter the code", true, true,
                List.of("microsoft"), List.of("code", "code"));

        context.assertFalse(oneWeakTerm.block(), Text.literal("One weak term must not block"));
        context.assertTrue(twoWeakTerms.block(), Text.literal("Two distinct weak terms must block"));
        context.assertFalse(duplicatedConfigTerm.block(), Text.literal("Duplicate keywords are one signal"));
        context.complete();
    }

    @GameTest
    public void respectsWordBoundariesAndDisabledRules(TestContext context) {
        DialogRiskAnalyzer.Verdict embeddedTerm = DialogRiskAnalyzer.inspect(
                false, "OAuth connection", true, true,
                List.of("auth"), List.of());
        DialogRiskAnalyzer.Verdict disabledPolicy = DialogRiskAnalyzer.inspect(
                true, "Microsoft password", false, false,
                List.of("microsoft", "password"), List.of("login", "verification"));

        context.assertFalse(embeddedTerm.block(), Text.literal("A keyword inside another word must not match"));
        context.assertFalse(disabledPolicy.block(), Text.literal("Disabled policy rules must allow the dialog"));
        context.complete();
    }
}
