package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Situational awareness: animated warnings for approaching players, bows aimed at you,
 * low health, breaking armor, totem pops and fusing creepers.
 */
public class Guardian extends Module {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final NumberSetting radius = add(new NumberSetting("Approach radius", 16, 4, 48, 1));
    private final BoolSetting aim = add(new BoolSetting("Bow aim warning", true));
    private final NumberSetting lowHp = add(new NumberSetting("Low health", 6, 0, 16, 1));
    private final NumberSetting armorPct = add(new NumberSetting("Armor warning %", 10, 0, 50, 5));
    private final BoolSetting totems = add(new BoolSetting("Totem warnings", true));
    private final BoolSetting creepers = add(new BoolSetting("Creeper warning", true));
    private final BoolSetting vignette = add(new BoolSetting("Danger vignette", true));
    private final BoolSetting sound = add(new BoolSetting("Sound", true));

    private static final class Alert {
        final String key;
        String text;
        final int color;
        final long start = System.currentTimeMillis();
        long seen = start;

        Alert(String key, String text, int color) {
            this.key = key;
            this.text = text;
            this.color = color;
        }
    }

    private final List<Alert> alerts = new ArrayList<>();
    private final Map<String, Long> lastFired = new HashMap<>();
    private final Map<String, Double> lastDist = new HashMap<>();
    private int lastTotems = -1;
    private boolean danger;

    public Guardian() {
        super("Guardian", "Warns you about threats: players, bows, low HP, armor, totems, creepers.", Category.MISC);
    }

    @Override
    protected void onDisable() {
        alerts.clear();
        lastDist.clear();
        lastTotems = -1;
    }

    private void raise(String key, String text, int color, long hold) {
        long now = System.currentTimeMillis();
        for (Alert a : alerts) {
            if (a.key.equals(key)) {
                a.text = text;
                a.seen = now + hold;
                return;
            }
        }
        Alert a = new Alert(key, text, color);
        a.seen = now + hold;
        alerts.add(a);
        Long lf = lastFired.get(key);
        if (sound.get() && (lf == null || now - lf > 4000)) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.6f, 0.7f));
        }
        lastFired.put(key, now);
    }

    private static boolean isRanged(ItemStack s) {
        Item i = s.getItem();
        return i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        Vec3d myEye = mc.player.getEyePos();
        Map<String, Double> dists = new HashMap<>();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || Friends.is(p) || p.isSpectator()) continue;
            String name = p.getGameProfile().getName();
            double d = mc.player.distanceTo(p);
            dists.put(name, d);
            Double prev = lastDist.get(name);
            if (d < radius.d() && prev != null && d < prev - 0.02) {
                raise("near:" + name, "⚠ " + name + " approaching · " + Math.round(d) + "m", 0xFFFFB84C, 600);
            }
            if (aim.get() && p.isUsingItem() && isRanged(p.getActiveItem()) && d < 64) {
                Vec3d look = p.getRotationVec(1f);
                Vec3d to = myEye.subtract(p.getEyePos()).normalize();
                double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, look.dotProduct(to)))));
                if (angle < 7) raise("aim:" + name, "➹ " + name + " is aiming at you!", 0xFFFF4C5C, 300);
            }
        }
        lastDist.clear();
        lastDist.putAll(dists);

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        danger = hp > 0 && hp <= lowHp.d();
        if (danger) raise("hp", String.format(Locale.ROOT, "❤ Low health: %.1f", hp), 0xFFFF4C5C, 300);

        if (armorPct.d() > 0) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack s = mc.player.getEquippedStack(slot);
                if (s.isEmpty() || !s.isDamageable()) continue;
                int pct = Math.round(100f * (1f - s.getDamage() / (float) s.getMaxDamage()));
                if (pct <= armorPct.d()) raise("armor:" + slot, "⛨ " + s.getName().getString() + " at " + pct + "%", 0xFFFFB84C, 300);
            }
        }

        if (totems.get()) {
            int count = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? mc.player.getOffHandStack().getCount() : 0;
            for (int i = 0; i < 36; i++) {
                ItemStack s = mc.player.getInventory().getStack(i);
                if (s.isOf(Items.TOTEM_OF_UNDYING)) count += s.getCount();
            }
            if (lastTotems > 0 && count < lastTotems) {
                raise("totem", count == 0 ? "✚ Totem popped — none left!" : "✚ Totem popped · " + count + " left", 0xFFFFD36E, 3000);
            }
            lastTotems = count;
        }

        if (creepers.get()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof CreeperEntity c && mc.player.distanceTo(c) < 6 && c.getClientFuseTime(1f) > 0.05f) {
                    raise("creeper", "✸ Creeper is about to explode!", 0xFFFF4C5C, 300);
                    break;
                }
            }
        }
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        if (vignette.get() && danger) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 180.0);
            int c = Theme.alpha(0xFF2030, (int) (90 + 70 * pulse));
            int clear = 0x00FF2030;
            int d = 50;
            ctx.fillGradient(0, 0, sw, d, c, clear);
            ctx.fillGradient(0, sh - d, sw, sh, clear, c);
            Rounded.gradientH(ctx, 0, 0, d, sh, 0, c, clear);
            Rounded.gradientH(ctx, sw - d, 0, sw, sh, 0, clear, c);
        }

        TextRenderer tr = mc.textRenderer;
        int y = 40;
        Iterator<Alert> it = alerts.iterator();
        while (it.hasNext()) {
            Alert a = it.next();
            float in = Fx.easeOutBack((now - a.start) / 280f);
            float out = now > a.seen ? Fx.easeOutCubic((now - a.seen) / 300f) : 0f;
            if (out >= 1f) {
                it.remove();
                continue;
            }
            float vis = Math.max(0f, Math.min(1f, in) - out);
            Text text = Text.literal(a.text).formatted(Formatting.BOLD);
            int w = tr.getWidth(text) + 20, h = 18;
            int x = sw / 2 - w / 2;
            int yy = (int) (y - (1 - in) * 10);
            float pulse = 0.6f + 0.4f * (float) Math.sin(now / 150.0);
            Rounded.shadow(ctx, x, yy, x + w, yy + h, 7, 6, Theme.alpha(a.color, (int) (70 * vis * pulse)));
            Rounded.fill(ctx, x, yy, x + w, yy + h, 7, Theme.fade(0xE60E0E14, vis));
            Rounded.outline(ctx, x, yy, x + w, yy + h, 7, Theme.alpha(a.color, (int) (220 * vis)));
            if (!Fx.invisible(vis)) ctx.drawCenteredTextWithShadow(tr, text, sw / 2, yy + 5, Theme.fade(a.color, vis));
            y += (int) ((h + 4) * (1 - out));
        }
    }
}
