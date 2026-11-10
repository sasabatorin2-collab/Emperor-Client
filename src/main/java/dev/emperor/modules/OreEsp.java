package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Highlights ores through walls (scans loaded chunks, no texture-pack tricks needed). */
public class OreEsp extends Module {
    private static final class Group {
        final BoolSetting on;
        final int color;

        Group(BoolSetting on, int color) {
            this.on = on;
            this.color = color;
        }
    }

    private final NumberSetting range = add(new NumberSetting("Range", 64, 16, 128, 8));
    private final NumberSetting perTick = add(new NumberSetting("Chunks/tick", 1, 1, 4, 1));

    private final Map<Block, Group> groups = new HashMap<>();
    private final Map<Long, WorldChunk> scanned = new HashMap<>();
    private final Map<Long, List<BlockPos>> hits = new HashMap<>();
    private ClientWorld lastWorld;

    public OreEsp() {
        super("OreESP", "Highlights ores and spawners through walls.", Category.RENDER);
        group("Ancient Debris", true, 0xFFC2753A, Blocks.ANCIENT_DEBRIS);
        group("Diamond", true, 0xFF55FFFF, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        group("Emerald", false, 0xFF55FF55, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        group("Gold", false, 0xFFFFE04A, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        group("Iron", false, 0xFFE0C9B0, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        group("Redstone", false, 0xFFFF5555, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        group("Lapis", false, 0xFF4C6CFF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        group("Spawner", true, 0xFFB44CFF, Blocks.SPAWNER);
    }

    private void group(String name, boolean def, int color, Block... blocks) {
        Group g = new Group(add(new BoolSetting(name, def)), color);
        for (Block b : blocks) groups.put(b, g);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            scanned.clear();
            hits.clear();
        }

        int cr = (int) Math.ceil(range.d() / 16.0) + 1;
        ChunkPos cp = mc.player.getChunkPos();
        List<WorldChunk> todo = new ArrayList<>();
        for (int dx = -cr; dx <= cr; dx++) {
            for (int dz = -cr; dz <= cr; dz++) {
                WorldChunk c = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
                if (c == null) continue;
                if (scanned.get(c.getPos().toLong()) == c) continue;
                todo.add(c);
            }
        }
        todo.sort((a, b) -> Integer.compare(dist(a, cp), dist(b, cp)));
        for (int i = 0; i < Math.min(perTick.i(), todo.size()); i++) scan(todo.get(i));
    }

    private static int dist(WorldChunk c, ChunkPos p) {
        int dx = c.getPos().x - p.x;
        int dz = c.getPos().z - p.z;
        return dx * dx + dz * dz;
    }

    private void scan(WorldChunk c) {
        List<BlockPos> list = new ArrayList<>();
        ChunkSection[] sections = c.getSectionArray();
        int minY = c.getBottomY();
        int x0 = c.getPos().getStartX();
        int z0 = c.getPos().getStartZ();
        for (int si = 0; si < sections.length; si++) {
            ChunkSection sec = sections[si];
            if (sec == null || sec.isEmpty()) continue;
            int y0 = minY + si * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block b = sec.getBlockState(x, y, z).getBlock();
                        if (groups.containsKey(b)) list.add(new BlockPos(x0 + x, y0 + y, z0 + z));
                    }
                }
            }
        }
        long key = c.getPos().toLong();
        hits.put(key, list);
        scanned.put(key, c);
    }

    @Override
    public void onRender3d(float delta) {
        if (mc.world == null || mc.player == null) return;
        double max = range.d() * range.d();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int drawn = 0;
        for (List<BlockPos> list : hits.values()) {
            for (BlockPos pos : list) {
                if (drawn > 4000) return;
                if (pos.getSquaredDistance(px, py, pz) > max) continue;
                Group g = groups.get(mc.world.getBlockState(pos).getBlock());
                if (g == null || !g.on.get()) continue;
                RenderUtil.box(new Box(pos), g.color, true, true);
                drawn++;
            }
        }
    }

    @Override
    protected void onDisable() {
        scanned.clear();
        hits.clear();
    }
}
