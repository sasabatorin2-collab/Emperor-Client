package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Chat;
import dev.emperor.util.RenderUtil;
import dev.emperor.util.WorldScan;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds player bases by scoring "unnatural" block entities (storage, beds, signs, beacons...)
 * per chunk. Any chunk at or above the threshold is flagged, announced in chat and logged to
 * config/emperor_bases.txt.
 */
public class BaseFinder extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Score threshold", 8, 3, 40, 1));
    private final BoolSetting alerts = add(new BoolSetting("Chat alerts", true));
    private final BoolSetting save = add(new BoolSetting("Save to file", true));
    private final BoolSetting render = add(new BoolSetting("Render", true));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", true));

    private static final class Base {
        final List<BlockPos> blocks = new ArrayList<>();
        int score;
        BlockPos center;
    }

    private final Map<Long, Base> bases = new HashMap<>();
    private final Set<Long> announced = new HashSet<>();
    private ClientWorld lastWorld;
    private long lastScan = -100;

    public BaseFinder() {
        super("BaseFinder", "Detects bases by counting storage, beds and signs per chunk.", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return String.valueOf(bases.size());
    }

    private static int weight(BlockEntity be) {
        if (be instanceof BeaconBlockEntity) return 6;
        if (be instanceof ShulkerBoxBlockEntity || be instanceof EnchantingTableBlockEntity || be instanceof BrewingStandBlockEntity)
            return 2;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof EnderChestBlockEntity
                || be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity
                || be instanceof DispenserBlockEntity || be instanceof BedBlockEntity || be instanceof SignBlockEntity)
            return 1;
        return 0;
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            bases.clear();
            announced.clear();
        }
        long t = mc.world.getTime();
        if (t - lastScan < 20 && t >= lastScan) return;
        lastScan = t;

        Map<Long, Base> fresh = new HashMap<>();
        for (BlockEntity be : WorldScan.blockEntities()) {
            int w = weight(be);
            if (w == 0) continue;
            long key = new ChunkPos(be.getPos()).toLong();
            Base b = fresh.computeIfAbsent(key, k -> new Base());
            b.score += w;
            b.blocks.add(be.getPos());
        }

        bases.clear();
        for (Map.Entry<Long, Base> e : fresh.entrySet()) {
            Base b = e.getValue();
            if (b.score < threshold.i()) continue;
            long x = 0, y = 0, z = 0;
            for (BlockPos p : b.blocks) {
                x += p.getX();
                y += p.getY();
                z += p.getZ();
            }
            int n = b.blocks.size();
            b.center = new BlockPos((int) (x / n), (int) (y / n), (int) (z / n));
            bases.put(e.getKey(), b);

            if (announced.add(e.getKey())) {
                String msg = "Base found at " + b.center.getX() + " " + b.center.getY() + " " + b.center.getZ()
                        + " (score " + b.score + ")";
                if (alerts.get()) Chat.info(msg);
                if (save.get()) log(b);
            }
        }
    }

    private void log(Base b) {
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("emperor_bases.txt");
            String dim = mc.world.getRegistryKey().getValue().toString();
            String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
            String line = server + " | " + dim + " | " + b.center.getX() + " " + b.center.getY() + " "
                    + b.center.getZ() + " | score " + b.score + System.lineSeparator();
            Files.writeString(f, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onRender3d(float delta) {
        if (!render.get() || mc.player == null) return;
        Vec3d start = RenderUtil.lookStart();
        for (Map.Entry<Long, Base> e : bases.entrySet()) {
            Base b = e.getValue();
            for (BlockPos p : b.blocks) {
                RenderUtil.box(new Box(p).contract(0.1), 0xFFFF5555, true, false);
            }
            int cx = ChunkPos.getPackedX(e.getKey()) * 16;
            int cz = ChunkPos.getPackedZ(e.getKey()) * 16;
            RenderUtil.box(new Box(cx, b.center.getY() - 0.5, cz, cx + 16, b.center.getY() + 0.5, cz + 16),
                    0xFFFFFF55, false, true);
            if (tracers.get()) {
                RenderUtil.line(start.x, start.y, start.z, cx + 8, b.center.getY(), cz + 8, 0xFFFFFF55);
            }
        }
    }
}
