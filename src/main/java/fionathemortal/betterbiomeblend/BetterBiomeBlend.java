package fionathemortal.betterbiomeblend;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(BetterBiomeBlend.MOD_ID)
public final class BetterBiomeBlend 
{
	public static final String MOD_ID = "betterbiomeblend";
	
	public
	BetterBiomeBlend()
	{
        ModLoadingContext.get().registerExtensionPoint(
    		ExtensionPoint.DISPLAYTEST, 
    		() -> Pair.of(
				() -> "client-only", 
				(v, n) -> n)
    		);

        DistExecutor.unsafeRunWhenOn(
    		Dist.CLIENT, 
    		() -> 
	    		() -> 
	    		{
	    			MinecraftForge.EVENT_BUS.register(BetterBiomeBlendClient.class);
	    			
	    			BetterBiomeBlendClient.overwriteOptifineGUIBlendRadiusOption();
				}
    		);
	}
}