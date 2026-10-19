package dev.dialogguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shared state + user notification (log, toast, chat line, HUD banner). */
public final class GuardState {
    private GuardState() {}

    private static volatile long bannerUntilMs = 0L;
    private static volatile String bannerDetail = "";
    private static long lastReportMs = 0L;
    private static String lastReason = "";

    public static boolean isBannerActive() {
        return GuardConfig.get().showBanner && System.currentTimeMillis() < bannerUntilMs;
    }

    public static String bannerDetail() {
        return bannerDetail;
    }

    public static void clear() {
        bannerUntilMs = 0L;
        bannerDetail = "";
    }

    /**
     * Called from any thread (packet handling may run on the Netty thread).
     * All UI work is bounced to the render thread.
     */
    public static void report(String source, String reason) {
        synchronized (GuardState.class) {
            long now = System.currentTimeMillis();
            // Collapse duplicate reports (e.g. packet handler re-run on the main thread).
            if (reason.equals(lastReason) && now - lastReportMs < 750) return;
            lastReason = reason;
            lastReportMs = now;
        }

        DialogGuardClient.LOGGER.warn("[DialogGuard] Blocked server dialog via {}: {}", source, reason);
        GuardConfig cfg = GuardConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.execute(() -> {
            bannerDetail = "Blocked a server dialog (" + reason + ").";
            bannerUntilMs = System.currentTimeMillis() + cfg.bannerSeconds * 1000L;

            if (cfg.showToast) {
                SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.literal("DialogGuard: dialog blocked"),
                        Text.literal(WarningBanner.HEADLINE));
            }
            if (cfg.echoToChat && mc.player != null) {
                mc.player.sendMessage(Text.literal(WarningBanner.HEADLINE)
                        .formatted(Formatting.RED, Formatting.BOLD), false);
                mc.player.sendMessage(Text.literal("[DialogGuard] " + bannerDetail)
                        .formatted(Formatting.GRAY), false);
            }
        });
    }
}
