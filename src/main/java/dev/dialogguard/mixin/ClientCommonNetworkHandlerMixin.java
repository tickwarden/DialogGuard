package dev.dialogguard.mixin;

import dev.dialogguard.DialogInspector;
import dev.dialogguard.GuardState;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 1 - packet interception.
 *
 * Packet flow:
 *   Server --ShowDialogS2CPacket--> Netty thread --> ClientCommonNetworkHandler.onShowDialog(...)
 *       --> (vanilla) showDialog(...) --> MinecraftClient.setScreen(new DialogScreen)
 *
 * ClientCommonNetworkHandler is the shared superclass of the play-phase and configuration-phase
 * handlers, so one injection covers both phases and every 1.21.6+ version.
 *
 * We inject at HEAD and cancel, so the dialog screen is never even created. The handler may be
 * entered once on the network thread and again on the main thread; cancelling on the first
 * entry means the second never happens, and GuardState de-duplicates anyway.
 */
@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {

    @Inject(method = "onShowDialog", at = @At("HEAD"), cancellable = true)
    private void dialogguard$onShowDialog(ShowDialogS2CPacket packet, CallbackInfo ci) {
        try {
            DialogInspector.Verdict verdict = DialogInspector.inspect(packet.dialog().value());
            if (verdict.block()) {
                ci.cancel(); // drop the packet: no screen, no input fields, nothing to submit
                GuardState.report("show_dialog packet", verdict.reason());
            }
        } catch (Throwable t) {
            // Fail closed: if we cannot analyse a server dialog, do not show it.
            ci.cancel();
            GuardState.report("show_dialog packet", "dialog could not be analysed (" + t.getClass().getSimpleName() + ")");
        }
    }
}
