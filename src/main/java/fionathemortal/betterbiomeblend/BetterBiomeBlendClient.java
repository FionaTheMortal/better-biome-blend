package fionathemortal.betterbiomeblend;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
public final class BetterBiomeBlendClient
{
    public static final int BIOME_BLEND_RADIUS_MAX = 14;
    public static final int BIOME_BLEND_RADIUS_MIN = 0;

    @SubscribeEvent
    public static void
    onChunkLoadedEvent(ChunkEvent.Load event)
    {
        Chunk chunk = event.getChunk();
        World world = event.getWorld();

        ColorChunkCache cache = BiomeColor.getColorChunkCacheForWorld(world);

        if (cache != null)
        {
            cache.invalidateNeighbourhood(chunk.x, chunk.z);
        }
    }

    @SubscribeEvent
    public static void
    onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            Minecraft minecraft = Minecraft.getMinecraft();
            World world = minecraft.world;

            if (world == null)
            {
                SereneSeasonsCompat.reset();
            }
            else if (SereneSeasonsCompat.hasSubSeasonChanged())
            {
                ColorChunkCache cache = BiomeColor.getColorChunkCacheForWorld(world);

                if (cache != null)
                {
                    cache.invalidateAll();
                }
            }
        }
    }

    @SubscribeEvent
    public static void
    onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(BetterBiomeBlend.MOD_ID))
        {
            ConfigManager.sync(BetterBiomeBlend.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
