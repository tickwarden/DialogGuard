package dev.dialogguard;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Connection-lifecycle hooks built on Fabric's client networking API.
 *
 * IMPORTANT - packet flow:
 *   Vanilla dialogs travel as the built-in S2C packet "show_dialog" (play AND configuration phase),
 *   NOT as a custom payload. ClientPlayNetworking only dispatches custom-payload channels, so it
 *   cannot see that packet. Interception therefore happens in
 *   {@link dev.dialogguard.mixin.ClientCommonNetworkHandlerMixin}. This class handles what
 *   ClientPlayNetworking / the connection events are good for: resetting state per server session.
 */
public final class DialogGuardNetworking {
    private DialogGuardNetworking() {}

    public static void register() {
        // Play phase: leaving a server clears any banner left over from it.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GuardState.clear());
        // Configuration phase (dialogs can already be sent here, before the player joins the world).
        ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> GuardState.clear());
    }
}
