package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class HitSounds extends Module {
    private final ModeSetting sound = add(new ModeSetting("Sound", "Ding", "Ding", "Bell", "Chime", "Click", "Arrow"));
    private final NumberSetting volume = add(new NumberSetting("Volume", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting pitch = add(new NumberSetting("Pitch", 1.4, 0.5, 2.0, 0.05));

    public HitSounds() {
        super("HitSounds", "Plays a satisfying sound every time you hit something.", Category.MISC);
    }

    public static void onHit() {
        HitSounds h = ModuleManager.get(HitSounds.class);
        if (h == null || !h.isEnabled()) return;
        SoundEvent e = switch (h.sound.get()) {
            case "Bell" -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case "Chime" -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case "Click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "Arrow" -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(e, h.pitch.f(), h.volume.f()));
    }
}
