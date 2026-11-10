package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Your 27 main inventory slots, always visible. */
public class InventoryElement extends HudElement {
    private static final int S = 18;

    public InventoryElement() {
        super("Inventory", false, 1, 0, 2, 64);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return 9 * S + 8;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 3 * S + 8;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        panel(ctx, x, y, w, h, 1f);
        for (int i = 0; i < 27; i++) {
            int sx = x + 4 + (i % 9) * S, sy = y + 4 + (i / 9) * S;
            ctx.fill(sx, sy, sx + S - 2, sy + S - 2, 0x30FFFFFF);
            ItemStack st = mc.player.getInventory().getStack(9 + i);
            if (st.isEmpty()) continue;
            ctx.drawItem(st, sx, sy);
            ctx.drawStackOverlay(tr, st, sx, sy);
        }
    }
}
