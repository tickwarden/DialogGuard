package dev.dialogguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shared state + user notification (log, toast, chat line, HUD banner). */
public final class GuardState {
    private GuardState() {}

    public record SessionSummary(long blockedCount, long lastBlockedAtMs, String lastReason) {}

    private static volatile long bannerUntilMs = 0L;
    private static volatile String bannerDetail = "";

    // Accessed under GuardState.class so reports arriving on Netty and the client thread agree.
    private static long blockedCount = 0L;
    private static long lastBlockedAtMs = 0L;
    private static String lastBlockedReason = "";
    private static long lastReportMs = 0L;
    private static String lastReportedReason = "";
    private static long lastNotificationMs = 0L;

    public static boolean isBannerActive() {
        return GuardConfig.get().showBanner && System.currentTimeMillis() < bannerUntilMs;
    }

    public static String bannerDetail() {
        return bannerDetail;
    }

    public static SessionSummary sessionSummary() {
        synchronized (GuardState.class) {
            return new SessionSummary(blockedCount, lastBlockedAtMs, lastBlockedReason);
        }
    }

    /** Clears the banner and all per-server session state on disconnect. */
    public static void clear() {
        synchronized (GuardState.class) {
            bannerUntilMs = 0L;
            bannerDetail = "";
            resetSessionStatsLocked();
        }
    }

    /** Clears the counters shown by /dialogguard status without hiding an active warning banner. */
    public static void resetSessionStats() {
        synchronized (GuardState.class) {
            resetSessionStatsLocked();
        }
    }

    private static void resetSessionStatsLocked() {
        blockedCount = 0L;
        lastBlockedAtMs = 0L;
        lastBlockedReason = "";
        lastReportMs = 0L;
        lastReportedReason = "";
        lastNotificationMs = 0L;
    }

    /**
     * Called from any thread (packet handling may run on the Netty thread).
     * All UI work is bounced to the render thread. Chat/toast alerts are throttled, while the
     * banner and session counter continue to reflect every non-duplicate blocked dialog.
     */
    public static void report(String source, String reason) {
        GuardConfig cfg = GuardConfig.get();
        long now = System.currentTimeMillis();
        boolean sendNotification;

        synchronized (GuardState.class) {
            // Collapse duplicate reports (e.g. a packet and its screen-level fallback).
            if (reason.equals(lastReportedReason) && now - lastReportMs < 750) return;
            lastReportedReason = reason;
            lastReportMs = now;

            blockedCount++;
            lastBlockedAtMs = now;
            lastBlockedReason = reason;

            long cooldownMs = Math.max(0, cfg.notificationCooldownSeconds) * 1000L;
            sendNotification = cooldownMs == 0L || lastNotificationMs == 0L
                    || now - lastNotificationMs >= cooldownMs;
            if (sendNotification) lastNotificationMs = now;
        }

        DialogGuardClient.LOGGER.warn("[DialogGuard] Blocked server dialog via {}: {}", source, reason);
        MinecraftClient mc = MinecraftClient.getInstance();
        String detail = "Blocked a server dialog (" + reason + ").";

        mc.execute(() -> {
            bannerDetail = detail;
            bannerUntilMs = System.currentTimeMillis() + cfg.bannerSeconds * 1000L;

            if (!sendNotification) return;
            if (cfg.showToast) {
                SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.literal("DialogGuard: dialog blocked"),
                        Text.literal(WarningBanner.HEADLINE));
            }
            if (cfg.echoToChat && mc.player != null) {
                mc.player.sendMessage(Text.literal(WarningBanner.HEADLINE)
                        .formatted(Formatting.RED, Formatting.BOLD), false);
                mc.player.sendMessage(Text.literal("[DialogGuard] " + detail)
                        .formatted(Formatting.GRAY), false);
            }
        });
    }
}
