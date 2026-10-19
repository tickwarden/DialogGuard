package dev.dialogguard;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.List;

/** Disables, clears and hides every text input found in a widget tree. */
public final class InputNeutralizer {
    private InputNeutralizer() {}

    /** @return number of text inputs that were disabled */
    public static int neutralize(List<? extends Element> elements) {
        int count = 0;
        for (Element element : elements) {
            if (element instanceof TextFieldWidget field) {          // single-line input
                field.setFocused(false);
                field.setEditable(false);
                field.setText("");
                field.active = false;
                field.visible = false;
                count++;
            } else if (element instanceof EditBoxWidget box) {        // multi-line input
                box.setFocused(false);
                box.active = false;
                box.visible = false;
                count++;
            }
            if (element instanceof ParentElement parent) {
                count += neutralize(parent.children());
            }
        }
        return count;
    }
}
