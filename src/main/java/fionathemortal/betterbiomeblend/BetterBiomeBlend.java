package fionathemortal.betterbiomeblend;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = BetterBiomeBlend.MOD_ID,
    name = BetterBiomeBlend.MOD_NAME,
    version = Tags.VERSION,
    acceptableRemoteVersions = "*")
public class BetterBiomeBlend {

    public static final String MOD_ID = "betterbiomeblend";
    public static final String MOD_NAME = "Better Biome Blend";
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    private final BetterBiomeBlendClient clientEvents = new BetterBiomeBlendClient();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BetterBiomeBlendConfig.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(clientEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(clientEvents);
    }
}
