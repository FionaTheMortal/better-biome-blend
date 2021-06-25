package fionathemortal.betterbiomeblend;

import net.minecraftforge.common.config.Config;

@Config(modid = BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlendConfig
{
    @Config.Name("Blend Radius")
    @Config.RangeInt(
        min = BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MIN,
        max = BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MAX)
    @Config.RequiresWorldRestart
    public static int blendRadius = 14;
}
