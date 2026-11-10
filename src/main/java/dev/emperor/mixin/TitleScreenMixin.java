package dev.emperor.mixin;

import dev.emperor.EmperorClient;
import dev.emperor.hud.Theme;
import dev.emperor.util.Rounded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void emperor$branding(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String name = EmperorClient.NAME + " Client";
        String sub = "v" + EmperorClient.VERSION + "  ·  Right Shift opens the menu";
        int w = Math.max(tr.getWidth(Text.literal("♛ " + name).formatted(Formatting.BOLD)), tr.getWidth(sub)) + 16;
        int x = 6, y = 6;
        Rounded.shadow(ctx, x, y, x + w, y + 30, 7, 6, 0x50000000);
        Rounded.fill(ctx, x, y, x + w, y + 30, 7, 0xB80E0E14);
        Rounded.gradientH(ctx, x + 5, y, x + w - 5, y + 1, 0, Theme.gradient(0f), Theme.gradient(1f));
        Rounded.outline(ctx, x, y, x + w, y + 30, 7, Theme.alpha(Theme.main(), 0x40));

        int cx = x + 8;
        Text crown = Text.literal("♛ ").formatted(Formatting.BOLD);
        ctx.drawTextWithShadow(tr, crown, cx, y + 6, Theme.primary());
        cx += tr.getWidth(crown);
        for (int i = 0; i < name.length(); i++) {
            Text ch = Text.literal(String.valueOf(name.charAt(i))).formatted(Formatting.BOLD);
            ctx.drawTextWithShadow(tr, ch, cx, y + 6, Theme.accent(i * 2));
            cx += tr.getWidth(ch);
        }
        ctx.drawTextWithShadow(tr, sub, x + 8, y + 18, 0xFF8C8C9A);
    }
}
