package fionathemortal.betterbiomeblend;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = BetterBiomeBlend.MOD_ID)
@Mod.EventBusSubscriber()
public class BetterBiomeBlend
{
    public static final String MOD_ID = "betterbiomeblend";
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    // TODO: This probably needs to be moved later when we make this a client-side only mod

    @SubscribeEvent
    public static void
    onChunkLoadedEvent(ChunkEvent.Load event)
    {
        Chunk chunk = event.getChunk();
        World world = event.getWorld();

        ColorChunkCache cache = null;

        if (world instanceof ColorChunkCacheProvider)
        {
            cache = ((ColorChunkCacheProvider) world).getColorChunkCache();
        }

        if (cache != null)
        {
            cache.invalidateNeighbourhood(chunk.x, chunk.z);
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
