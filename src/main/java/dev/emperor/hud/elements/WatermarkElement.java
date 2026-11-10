package dev.emperor.hud.elements;

import dev.emperor.EmperorClient;
import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkElement extends HudElement {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SEP = "  ⏐  ";
    private static final String CROWN = "♛ ";

    public WatermarkElement() {
        super("Watermark", true, 0, 5, 0, 5);
    }

    private String stats() {
        PlayerListEntry me = mc.getNetworkHandler() == null || mc.player == null ? null
                : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return mc.getCurrentFps() + " fps" + SEP + (me == null ? 0 : me.getLatency()) + " ms" + SEP + LocalTime.now().format(TIME);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return tr.getWidth(CROWN) + tr.getWidth(Text.literal(EmperorClient.NAME).formatted(Formatting.BOLD))
                + tr.getWidth(" " + EmperorClient.VERSION) + tr.getWidth(SEP) + tr.getWidth(stats()) + 14;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 17;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        panel(ctx, x, y, w, h, 1f);
        int cx = x + 7, ty = y + 5;
        ctx.drawTextWithShadow(tr, CROWN, cx, ty, Theme.primary());
        cx += tr.getWidth(CROWN);
        String name = EmperorClient.NAME;
        for (int i = 0; i < name.length(); i++) {
            Text ch = Text.literal(String.valueOf(name.charAt(i))).formatted(Formatting.BOLD);
            ctx.drawTextWithShadow(tr, ch, cx, ty, Theme.accent(i * 2));
            cx += tr.getWidth(ch);
        }
        String ver = " " + EmperorClient.VERSION;
        ctx.drawTextWithShadow(tr, ver, cx, ty, 0xFF8C8C9A);
        cx += tr.getWidth(ver);
        ctx.drawTextWithShadow(tr, SEP, cx, ty, 0xFF4A4A58);
        cx += tr.getWidth(SEP);
        ctx.drawTextWithShadow(tr, stats(), cx, ty, 0xFFE6E6EE);
    }
}
