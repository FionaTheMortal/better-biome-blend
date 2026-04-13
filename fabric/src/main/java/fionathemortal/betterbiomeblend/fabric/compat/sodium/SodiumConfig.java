package fionathemortal.betterbiomeblend.fabric.compat.sodium;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.ColorConfig;

public class SodiumConfig
{
    public static final ColorConfig[]
    configs =
    {
        new ColorConfig( 0, 4, 0, 0),
        new ColorConfig( 1, 4, 0, 0),
        new ColorConfig( 2, 3, 1, 0),
        new ColorConfig( 3, 3, 1, 0),
        new ColorConfig( 4, 3, 1, 0),
        new ColorConfig( 5, 3, 1, 0),
        new ColorConfig( 6, 2, 2, 0),
        new ColorConfig( 7, 2, 2, 2),
        new ColorConfig( 8, 2, 2, 2),
        new ColorConfig( 9, 2, 2, 0),
        new ColorConfig(10, 2, 2, 0),
        new ColorConfig(11, 2, 2, 2),
        new ColorConfig(12, 2, 2, 2),
        new ColorConfig(12, 2, 2, 2),
        new ColorConfig(12, 2, 2, 2)
    };

    public static ColorConfig
    getCurrentConfig()
    {
        ColorConfig result;

        int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();

        if (blendRadius >= 0 && blendRadius < configs.length)
        {
            result = configs[blendRadius];
        }
        else
        {
            result = configs[configs.length - 1];
        }

        return result;
    }
}
