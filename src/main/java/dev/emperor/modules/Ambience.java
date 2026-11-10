package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;

/** Client-side time of day and weather. */
public class Ambience extends Module {
    private final NumberSetting time = add(new NumberSetting("Time", 13000, 0, 24000, 250));
    private final BoolSetting clear = add(new BoolSetting("Clear weather", true));

    public Ambience() {
        super("Ambience", "Pick your own time of day and weather (client-side).", Category.RENDER);
    }

    @Override
    public String getInfo() {
        int h = (int) ((time.d() / 1000 + 6) % 24);
        int m = (int) (time.d() % 1000 * 60 / 1000);
        return String.format("%02d:%02d", h, m);
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        mc.world.getLevelProperties().setTimeOfDay((long) time.d());
        if (clear.get()) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }
}
