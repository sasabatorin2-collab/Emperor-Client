package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Projection;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Nametags extends Module {
    private static final EquipmentSlot[] ORDER = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

    private final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1));
    private final BoolSetting health = add(new BoolSetting("Health", true));
    private final BoolSetting distance = add(new BoolSetting("Distance", true));
    private final BoolSetting items = add(new BoolSetting("Items", true));

    public Nametags() {
        super("Nametags", "Stylish player nametags with health, distance and gear.", Category.RENDER);
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        if (mc.world == null || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        List<PlayerEntity> list = new ArrayList<>(mc.world.getPlayers());
        list.remove(mc.player);
        list.sort(Comparator.comparingDouble(p -> -mc.player.squaredDistanceTo((PlayerEntity) p)));

        for (PlayerEntity p : list) {
            if (p.isInvisible() && !p.isGlowing()) continue;
            Vec3d s = Projection.toScreen(p.getLerpedPos(delta).add(0, p.getHeight() + 0.55, 0));
            if (s == null) continue;

            String name = p.getName().getString();
            float hp = p.getHealth() + p.getAbsorptionAmount();
            String hpText = health.get() ? String.format(Locale.ROOT, " %.1f", hp) : "";
            String distText = distance.get() ? String.format(Locale.ROOT, " %.0fm", mc.player.distanceTo(p)) : "";
            int w = tr.getWidth(name) + tr.getWidth(hpText) + tr.getWidth(distText);
            boolean friend = Friends.is(p);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(s.x, s.y, 0);
            float sc = scale.f();
            ctx.getMatrices().scale(sc, sc, 1f);

            int x1 = -w / 2 - 5, x2 = w / 2 + 5;
            Rounded.fill(ctx, x1, -13, x2, 0, 4, 0xB00E0E14);
            int barW = x2 - x1 - 6;
            float frac = Math.min(1f, hp / Math.max(1f, p.getMaxHealth()));
            Rounded.gradientH(ctx, x1 + 3, -1, x1 + 3 + Math.max(1, (int) (barW * frac)), 0, 0,
                    friend ? Theme.primary() : Theme.gradient(0f), friend ? Theme.primary() : Theme.gradient(1f));

            int tx = -w / 2;
            ctx.drawTextWithShadow(tr, name, tx, -10, friend ? Theme.primary() : 0xFFFFFFFF);
            tx += tr.getWidth(name);
            if (!hpText.isEmpty()) {
                int col = frac > 0.66f ? 0xFF4CE08A : frac > 0.33f ? 0xFFFFD84C : 0xFFFF5C6C;
                ctx.drawTextWithShadow(tr, hpText, tx, -10, col);
                tx += tr.getWidth(hpText);
            }
            if (!distText.isEmpty()) ctx.drawTextWithShadow(tr, distText, tx, -10, 0xFF8C8C9A);

            if (items.get()) {
                List<ItemStack> gear = new ArrayList<>();
                for (EquipmentSlot slot : ORDER) {
                    ItemStack st = p.getEquippedStack(slot);
                    if (!st.isEmpty()) gear.add(st);
                }
                int ix = -gear.size() * 8;
                for (ItemStack st : gear) {
                    ctx.drawItem(st, ix, -31);
                    ctx.drawStackOverlay(tr, st, ix, -31);
                    ix += 16;
                }
            }
            ctx.getMatrices().pop();
        }
    }
}
