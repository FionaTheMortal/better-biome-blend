package fionathemortal.betterbiomeblend.forge.compat.embeddium;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.ColorGather;
import fionathemortal.betterbiomeblend.common.ColorGenContext;
import fionathemortal.betterbiomeblend.common.util.Array3i;
import me.jellysquid.mods.sodium.client.world.biome.BiomeSlice;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

public final class EmbeddiumColorGather
{
    private static int
    getColor(
        BiomeSlice    source,
        ColorResolver resolver,
        int           x,
        int           y,
        int           z)
    {
        Biome biome = source.getBiome(x, y, z).value();

        int result = resolver.getColor(biome, x, z);

        return result;
    }

    public static void
    gatherColors(
        BiomeSlice      source,
        ColorResolver   resolver,
        ColorGenContext context)
    {
        final ColorConfig blendConfig = context.blendConfig;

        int sampleMinX = context.sampleMinX;
        int sampleMinY = context.sampleMinY;
        int sampleMinZ = context.sampleMinZ;

        int sampleCountX = context.sampleCountX;
        int sampleCountY = context.sampleCountY;
        int sampleCountZ = context.sampleCountZ;

        for (int z = 0;
             z < sampleCountZ;
             ++z)
        {
            for (int y = 0;
                 y < sampleCountY;
                 ++y)
            {
                for (int x = 0;
                     x < sampleCountX;
                     ++x)
                {
                    int srcX = x + sampleMinX;
                    int srcY = y + sampleMinY;
                    int srcZ = z + sampleMinZ;

                    int srcBlockX = blendConfig.getBlockFromSample(srcX);
                    int srcBlockY = blendConfig.getBlockFromSample(srcY);
                    int srcBlockZ = blendConfig.getBlockFromSample(srcZ);

                    int blockX = ColorGather.getRandomSamplePosition(srcBlockX, blendConfig.sampleSizeLog2, ColorGather.SAMPLE_SEED_X);
                    int blockY = ColorGather.getRandomSamplePosition(srcBlockY, blendConfig.sampleSizeLog2, ColorGather.SAMPLE_SEED_Y);
                    int blockZ = ColorGather.getRandomSamplePosition(srcBlockZ, blendConfig.sampleSizeLog2, ColorGather.SAMPLE_SEED_Z);

                    int color = getColor(source, resolver, blockX, blockY, blockZ);

                    int dstIndex = Array3i.getArrayIndex(
                        sampleCountX,
                        sampleCountY,
                        x,
                        y,
                        z);

                    context.setColorSample(color, dstIndex);
                }
            }
        }
    }
}
