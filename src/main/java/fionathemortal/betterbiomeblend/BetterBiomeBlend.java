package fionathemortal.betterbiomeblend;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlend 
{
    public static final String MOD_ID = "example";

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        System.out.println("Hello world!");
    }
}
