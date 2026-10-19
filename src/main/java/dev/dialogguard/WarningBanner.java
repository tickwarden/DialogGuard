package dev.dialogguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Red warning box drawn at the top of the screen (HUD and every Screen). */
public final class WarningBanner {
    public static final String HEADLINE =
            "SECURITY WARNING: Servers can never safely request your Microsoft credentials or verification codes.";

    private WarningBanner() {}

    public static void render(DrawContext ctx) {
        if (!GuardState.isBannerActive()) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int screenW = ctx.getScaledWindowWidth();
        int boxW = Math.min(screenW - 16, 440);
        int textW = boxW - 16;

        List<OrderedText> lines = new ArrayList<>(tr.wrapLines(Text.literal(HEADLINE), textW));
        int headlineLines = lines.size();
        lines.addAll(tr.wrapLines(Text.literal(GuardState.bannerDetail()), textW));

        final int lineH = 10; // 9px font + 1px gap
        int boxH = lines.size() * lineH + 10;
        int x = (screenW - boxW) / 2;
        int y = 6;

        // Background + 1px white border (built from fills to avoid API drift between versions).
        ctx.fill(x, y, x + boxW, y + boxH, 0xE0A00000);
        ctx.fill(x, y, x + boxW, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + boxH - 1, x + boxW, y + boxH, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + boxH, 0xFFFFFFFF);
        ctx.fill(x + boxW - 1, y, x + boxW, y + boxH, 0xFFFFFFFF);

        int ty = y + 5;
        for (int i = 0; i < lines.size(); i++) {
            // Headline in white, detail in light grey.
            int color = i < headlineLines ? 0xFFFFFFFF : 0xFFE0E0E0;
            ctx.drawTextWithShadow(tr, lines.get(i), x + 8, ty, color);
            ty += lineH;
        }
    }
}
