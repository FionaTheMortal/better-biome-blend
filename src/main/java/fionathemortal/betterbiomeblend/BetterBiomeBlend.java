package fionathemortal.betterbiomeblend;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;

public class BetterBiomeBlend implements ModInitializer
{
	public static final String MOD_ID = "betterbiomeblend";
	
	@Override
	public void 
	onInitialize()
	{
		ScreenEvents.AFTER_INIT.register(BetterBiomeBlendClient::postInitGUIEvent);
		
		BetterBiomeBlendClient.overwriteOptifineGUIBlendRadiusOption();
	}
}