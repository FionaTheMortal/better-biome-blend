package fionathemortal.betterbiomeblend;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class BetterBiomeBlendClient {

    public static final int BIOME_BLEND_RADIUS_MAX = 14;
    public static final int BIOME_BLEND_RADIUS_MIN = 0;

    @SubscribeEvent
    public void onChunkLoadedEvent(ChunkEvent.Load event) {
        Chunk chunk = event.getChunk();
        World world = event.world;

        ColorChunkCache cache = BiomeColor.getColorChunkCacheForWorld(world);

        if (cache != null) {
            cache.invalidateNeighbourhood(chunk.xPosition, chunk.zPosition);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft minecraft = Minecraft.getMinecraft();
            World world = minecraft.theWorld;

            if (world == null) {
                SereneSeasonsCompat.reset();
            } else if (SereneSeasonsCompat.hasSubSeasonChanged()) {
                ColorChunkCache cache = BiomeColor.getColorChunkCacheForWorld(world);

                if (cache != null) {
                    cache.invalidateAll();
                }
            }
        }
    }

}
