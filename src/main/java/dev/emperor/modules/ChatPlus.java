package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ColorSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Chat timestamps and highlighting of messages that mention you. */
public class ChatPlus extends Module {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final BoolSetting timestamps = add(new BoolSetting("Timestamps", true));
    private final BoolSetting mentions = add(new BoolSetting("Highlight mentions", true));
    private final ColorSetting mentionColor = add(new ColorSetting("Mention colour", 0xFFFFD36E));
    private final BoolSetting mentionSound = add(new BoolSetting("Mention sound", true));

    public ChatPlus() {
        super("ChatPlus", "Timestamps and highlighted mentions in chat.", Category.MISC);
    }

    public static Text transform(Text message) {
        ChatPlus c = ModuleManager.get(ChatPlus.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.isEnabled() || message == null) return message;
        MutableText out = Text.empty();
        if (c.timestamps.get()) {
            out.append(Text.literal(LocalTime.now().format(TIME) + " ").formatted(Formatting.DARK_GRAY));
        }
        boolean mention = false;
        if (c.mentions.get() && mc.player != null) {
            String me = mc.player.getGameProfile().getName().toLowerCase(Locale.ROOT);
            String s = message.getString().toLowerCase(Locale.ROOT);
            // Ignore our own chat lines ("<Name> ...").
            mention = s.contains(me) && !s.startsWith("<" + me + ">");
        }
        if (mention) {
            out.append(Text.literal("▌ ").styled(st -> st.withColor(TextColor.fromRgb(c.mentionColor.get() & 0xFFFFFF))));
            out.append(message.copy().styled(st -> st.withBold(true)));
            if (c.mentionSound.get()) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.5f, 0.6f));
            }
        } else {
            out.append(message);
        }
        return out;
    }
}
