package dev.dialogguard;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/** Client-only status and configuration commands. */
public final class DialogGuardCommands {
    private DialogGuardCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("dialogguard")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal(
                                    "DialogGuard commands: status, reload, reset. Config: config/dialogguard.json"));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("status").executes(context -> {
                            GuardConfig cfg = GuardConfig.get();
                            GuardState.SessionSummary summary = GuardState.sessionSummary();
                            context.getSource().sendFeedback(Text.literal("DialogGuard: active"));
                            context.getSource().sendFeedback(Text.literal(
                                    "Input dialogs: " + onOff(cfg.blockInputDialogs)
                                            + " | sensitive-text scan: " + onOff(cfg.blockKeywordDialogs)
                                            + " | notification cooldown: " + cfg.notificationCooldownSeconds + "s"));
                            context.getSource().sendFeedback(Text.literal(
                                    "Blocked this session: " + summary.blockedCount()));
                            if (summary.blockedCount() > 0) {
                                long secondsAgo = Math.max(0L,
                                        (System.currentTimeMillis() - summary.lastBlockedAtMs()) / 1000L);
                                context.getSource().sendFeedback(Text.literal(
                                        "Last block: " + secondsAgo + "s ago — " + summary.lastReason()));
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(context -> {
                            boolean loaded = GuardConfig.load();
                            if (loaded) {
                                context.getSource().sendFeedback(Text.literal(
                                        "DialogGuard config reloaded from config/dialogguard.json."));
                                return 1;
                            }
                            context.getSource().sendError(Text.literal(
                                    "DialogGuard config could not be read; secure defaults are active. Check the log."));
                            return 0;
                        }))
                        .then(ClientCommandManager.literal("reset").executes(context -> {
                            GuardState.resetSessionStats();
                            context.getSource().sendFeedback(Text.literal(
                                    "DialogGuard session block counters have been reset."));
                            return 1;
                        }))));
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }
}
