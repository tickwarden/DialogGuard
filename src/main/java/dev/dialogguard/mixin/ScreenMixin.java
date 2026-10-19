package dev.dialogguard.mixin;

import dev.dialogguard.DialogInspector;
import dev.dialogguard.DialogRiskAnalyzer;
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
 * The packet interceptor does not see dialogs opened locally by a chat "show_dialog" click
 * event. Inspect the dialog model here as well, neutralize any inputs immediately, then return
 * blocked screens to their parent after init has finished.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void dialogguard$afterInit(MinecraftClient client, int width, int height, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof DialogScreen<?> dialogScreen)) return;

        DialogRiskAnalyzer.Verdict verdict;
        try {
            verdict = DialogInspector.inspect(((DialogScreenAccessor) self).dialogguard$getDialog());
        } catch (Throwable t) {
            // Fail closed if the dialog model cannot be inspected on this fallback route.
            verdict = new DialogRiskAnalyzer.Verdict(true,
                    "dialog could not be analysed (" + t.getClass().getSimpleName() + ")");
        }
        if (!verdict.block()) return;

        int disabled = InputNeutralizer.neutralize(self.children());
        GuardState.report("dialog screen", verdict.reason()
                + (disabled > 0 ? "; " + disabled + " text input field(s) disabled" : ""));

        // Defer setScreen to avoid re-entrant screen changes during init. The parent is retained
        // even in the configuration phase, when no world/player exists yet.
        client.execute(() -> {
            if (client.currentScreen == self) {
                client.setScreen(dialogScreen.getParentScreen());
            }
        });
    }
}
