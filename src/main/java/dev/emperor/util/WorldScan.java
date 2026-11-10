package dev.emperor.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/** Shared, throttled scan of block entities in all loaded chunks around the player. */
public final class WorldScan {
    private static final List<BlockEntity> CACHE = new ArrayList<>();
    private static ClientWorld lastWorld;
    private static long last = -100;

    private WorldScan() {
    }

    public static List<BlockEntity> blockEntities() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return CACHE;

        long t = mc.world.getTime();
        if (mc.world == lastWorld && t >= last && t - last < 10) return CACHE;
        lastWorld = mc.world;
        last = t;
        CACHE.clear();

        int r = mc.options.getViewDistance().getValue();
        ChunkPos cp = mc.player.getChunkPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                WorldChunk c = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
                if (c != null) CACHE.addAll(c.getBlockEntities().values());
            }
        }
        return CACHE;
    }
}
