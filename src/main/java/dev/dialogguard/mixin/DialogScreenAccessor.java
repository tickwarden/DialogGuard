package dev.dialogguard.mixin;

import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.dialog.type.Dialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the dialog backing a screen as a fallback for non-packet open routes. */
@Mixin(DialogScreen.class)
public interface DialogScreenAccessor {
    @Accessor("dialog")
    Dialog dialogguard$getDialog();
}
