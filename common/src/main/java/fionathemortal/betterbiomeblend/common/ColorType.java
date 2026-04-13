package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.compat.CustomColorResolver;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;

public final class ColorType
{
    public static final int GRASS   = 0;
    public static final int WATER   = 1;
    public static final int FOLIAGE = 2;

    public static final int FIRST = GRASS;
    public static final int LAST  = FOLIAGE;

    public static int
    getColorTypeFromResolver(ColorResolver resolver)
    {
        int result;

        if (resolver == BiomeColors.GRASS_COLOR_RESOLVER)
        {
            result = ColorType.GRASS;
        }
        else if (resolver == BiomeColors.WATER_COLOR_RESOLVER)
        {
            result = ColorType.WATER;
        }
        else if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER)
        {
            result = ColorType.FOLIAGE;
        }
        else
        {
            result = CustomColorResolver.getColorType(resolver);
        }

        return result;
    }
}
