package fionathemortal.betterbiomeblend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;

@Mod(BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlend 
{
	public static final String MOD_ID = "betterbiomeblend";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	public static int currentDimensionID;
	public static int blendRadius = 14;
	
	
}