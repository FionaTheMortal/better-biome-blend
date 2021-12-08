package fionathemortal.betterbiomeblend;

import fionathemortal.betterbiomeblend.core.CoreMod;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.optifine.CustomColors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = BetterBiomeBlend.MOD_ID, clientSideOnly = true)
public class BetterBiomeBlend
{
    public static final String MOD_ID   = "betterbiomeblend";
    public static final String MOD_NAME = "Better Biome Blend";
    public static final Logger LOGGER   = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    @Mod.EventHandler
    public void
    onPreInit(final FMLPreInitializationEvent event)
    {
        if (!CoreMod.foundMixinFramework)
        {
            MinecraftForge.EVENT_BUS.register(new MissingMixinFrameworkHandler());
        }
    }
}
