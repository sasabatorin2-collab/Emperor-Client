package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoElement extends HudElement {
    public InfoElement() {
        super("Info", true, 0, 5, 0, 28);
    }

    private List<String[]> lines() {
        List<String[]> lines = new ArrayList<>();
        var p = mc.player;
        boolean nether = mc.world != null && mc.world.getRegistryKey() == World.NETHER;
        double f = nether ? 8.0 : 1.0 / 8.0;
        double bps = Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20.0;
        Direction d = p.getHorizontalFacing();
        String axis = switch (d) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            default -> "+X";
        };
        lines.add(new String[]{"XYZ", String.format(Locale.ROOT, "%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ())});
        lines.add(new String[]{nether ? "Overworld" : "Nether", String.format(Locale.ROOT, "%.1f  %.1f", p.getX() * f, p.getZ() * f)});
        lines.add(new String[]{"Speed", String.format(Locale.ROOT, "%.2f b/s", bps)});
        lines.add(new String[]{"Facing", d.asString().substring(0, 1).toUpperCase(Locale.ROOT) + d.asString().substring(1) + " (" + axis + ")"});
        return lines;
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        int w = 0;
        for (String[] l : lines()) w = Math.max(w, tr.getWidth(l[0] + " " + l[1]));
        return w;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 4 * 10 - 1;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        int ly = y, i = 0;
        for (String[] l : lines()) {
            String label = l[0] + " ";
            int lx = rightAligned() ? x + w - tr.getWidth(label + l[1]) : x;
            ctx.drawTextWithShadow(tr, label, lx, ly, Theme.accent(i * 3));
            ctx.drawTextWithShadow(tr, l[1], lx + tr.getWidth(label), ly, 0xFFE6E6EE);
            ly += 10;
            i++;
        }
    }
}
