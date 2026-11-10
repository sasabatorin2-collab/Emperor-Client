package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/** Worn armor with colour-coded durability percentages. */
public class ArmorElement extends HudElement {
    private static final List<ItemStack> DUMMY = List.of(new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_LEGGINGS),
            new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_HELMET));

    public ArmorElement() {
        super("Armor", true, 1, 48, 2, 40);
    }

    private List<ItemStack> items(boolean preview) {
        List<ItemStack> armor = mc.player.getInventory().armor;
        return preview && !hasArmor() ? DUMMY : armor;
    }

    private boolean hasArmor() {
        for (ItemStack s : mc.player.getInventory().armor) if (!s.isEmpty()) return true;
        return false;
    }

    @Override
    public boolean hasContent() {
        return hasArmor();
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return 4 * 18 - 2;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 24;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        List<ItemStack> items = items(preview);
        int ix = x;
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                ctx.drawItem(s, ix, y);
                ctx.drawStackOverlay(tr, s, ix, y);
                if (s.isDamageable()) {
                    float f = 1f - s.getDamage() / (float) s.getMaxDamage();
                    int col = f > 0.6f ? 0xFF4CE08A : f > 0.3f ? 0xFFFFD84C : 0xFFFF5C6C;
                    String pct = Math.round(f * 100) + "%";
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(ix + 8, y + 17, 200);
                    ctx.getMatrices().scale(0.75f, 0.75f, 1f);
                    ctx.drawCenteredTextWithShadow(tr, pct, 0, 0, col);
                    ctx.getMatrices().pop();
                }
            }
            ix += 18;
        }
    }
}
