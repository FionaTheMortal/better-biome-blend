package fionathemortal.betterbiomeblend;

import net.minecraftforge.common.config.Configuration;

public class BetterBiomeBlendConfig {

    public static int blendRadius = 14;

    public static void init(java.io.File file) {
        Configuration config = new Configuration(file);

        try {
            config.load();

            blendRadius = config.getInt(
                "Blend Radius",
                Configuration.CATEGORY_GENERAL,
                blendRadius,
                BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MIN,
                BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MAX,
                "Biome color blending radius.");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
