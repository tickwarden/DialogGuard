package dev.dialogguard;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint. Wires up config, connection lifecycle listeners and the warning banner.
 *
 * Defence layers (see the mixin package):
 *   Layer 1: ClientCommonNetworkHandlerMixin - drops the S2C "show dialog" packet before any screen exists.
 *   Layer 2: ScreenMixin - catches dialogs that bypass the packet path
 *            (e.g. a chat "show_dialog" click event) and disables their text fields.
 */
public final class DialogGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "dialogguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        GuardConfig.load();
        DialogGuardNetworking.register();

        // Banner while in-game (HUD). HudRenderCallback is deprecated in newer Fabric API
        // releases but still present across the 1.21.6 - 1.21.11 range.
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> WarningBanner.render(drawContext));

        // Banner on top of every Screen as well, so it is visible during the
        // configuration phase / while another screen is open.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) ->
                        WarningBanner.render(drawContext)));

        LOGGER.info("DialogGuard loaded: server dialogs with input fields / credential prompts will be blocked.");
    }
}
