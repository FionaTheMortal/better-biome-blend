package fionathemortal.betterbiomeblend;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.optifine.CustomColors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = BetterBiomeBlend.MOD_ID, clientSideOnly = true)
public class BetterBiomeBlend
{
    public static final String MOD_ID = "betterbiomeblend";
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);
}
