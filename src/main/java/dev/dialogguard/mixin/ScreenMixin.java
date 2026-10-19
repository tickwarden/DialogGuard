package dev.dialogguard.mixin;

import dev.dialogguard.GuardConfig;
import dev.dialogguard.GuardState;
import dev.dialogguard.InputNeutralizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 2 - screen-level safety net.
 *
 * Some dialogs never use the show_dialog packet (e.g. a chat component with a "show_dialog"
 * click event opens the dialog locally). Those reach us as a DialogScreen being initialised.
 *
 * Screen.init(MinecraftClient, int, int) is the public entry that builds all widgets (it is
 * also re-run on window resize). We hook its TAIL, so the widget list is complete, then:
 *   1. disable + clear + hide every text field (read-only requirement), and
 *   2. when a world is loaded, close the screen on the next tick (avoids re-entrant setScreen).
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void dialogguard$afterInit(MinecraftClient client, int width, int height, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof DialogScreen<?>)) return;
        if (!GuardConfig.get().blockInputDialogs) return;

        int disabled = InputNeutralizer.neutralize(self.children());
        if (disabled == 0) return;

        GuardState.report("dialog screen", disabled + " text input field(s) disabled");

        // In the configuration phase there is no world; closing would drop to the title screen,
        // so there we only keep the (now inert) screen.
        if (client.world != null) {
            client.execute(() -> {
                if (client.currentScreen == self) {
                    client.setScreen(null);
                }
            });
        }
    }
}
