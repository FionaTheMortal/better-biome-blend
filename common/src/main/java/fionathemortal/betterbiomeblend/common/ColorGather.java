package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.Slice;
import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import fionathemortal.betterbiomeblend.common.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class ColorGather
{
    public static final int SAMPLE_SEED_X = 1664525;
    public static final int SAMPLE_SEED_Y = 214013;
    public static final int SAMPLE_SEED_Z = 16807;

    private static final int INVALID_COLOR = 0;

    public static Biome
    getBiomeForBlock(Level world, BlockPos blockPosition)
    {
        Biome result = null;

        Holder<Biome> biomeHolder = world.getBiome(blockPosition);

        if (biomeHolder.isBound())
        {
            result = biomeHolder.value();
        }

        return result;
    }

    public static int
    getColorForBlock(Level world, BlockPos blockPos, float posX, float posZ, ColorResolver colorResolver)
    {
        int result;

        Biome biome = getBiomeForBlock(world, blockPos);

        if (biome != null)
        {
            result = colorResolver.getColor(biome, posX, posZ);
        }
        else
        {
            result = Color.DEBUG_PINK;
        }

        return result;
    }

    public static int
    getRandomSamplePosition(int min, int blockSizeLog2, int seed)
    {
        int random = Random.noise(min, seed);
        int offset = Util.lowerBits(random, blockSizeLog2);
        int result = min + offset;

        return result;
    }

    private static void
    gatherColorsInSlice(
        Level           world,
        ColorResolver   resolver,
        ColorGenContext context,
        Slice           slice,
        int             sliceX,
        int             sliceY,
        int             sliceZ)
    {
        final ColorConfig blendConfig = context.blendConfig;

        int sliceSampleMinX = blendConfig.getSampleFromSlice(sliceX);
        int sliceSampleMinY = blendConfig.getSampleFromSlice(sliceY);
        int sliceSampleMinZ = blendConfig.getSampleFromSlice(sliceZ);

        int sliceSampleMaxX = blendConfig.getSampleFromSlice(sliceX + 1);
        int sliceSampleMaxY = blendConfig.getSampleFromSlice(sliceY + 1);
        int sliceSampleMaxZ = blendConfig.getSampleFromSlice(sliceZ + 1);

        int sampleMinX = Math.max(context.sampleMinX, sliceSampleMinX);
        int sampleMinY = Math.max(context.sampleMinY, sliceSampleMinY);
        int sampleMinZ = Math.max(context.sampleMinZ, sliceSampleMinZ);

        int sampleMaxX = Math.min(context.sampleMaxX, sliceSampleMaxX);
        int sampleMaxY = Math.min(context.sampleMaxY, sliceSampleMaxY);
        int sampleMaxZ = Math.min(context.sampleMaxZ, sliceSampleMaxZ);

        int sliceMinX = sampleMinX - sliceSampleMinX;
        int sliceMinY = sampleMinY - sliceSampleMinY;
        int sliceMinZ = sampleMinZ - sliceSampleMinZ;

        int blendMinX = sampleMinX - context.sampleMinX;
        int blendMinY = sampleMinY - context.sampleMinY;
        int blendMinZ = sampleMinZ - context.sampleMinZ;

        int sampleCountX = sampleMaxX - sampleMinX;
        int sampleCountY = sampleMaxY - sampleMinY;
        int sampleCountZ = sampleMaxZ - sampleMinZ;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

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
                    int sliceIndex = Array3i.getArrayIndex(
                        blendConfig.sliceSize,
                        blendConfig.sliceSize,
                        sliceMinX + x,
                        sliceMinY + y,
                        sliceMinZ + z);

                    int blendIndex = Array3i.getArrayIndex(
                        context.sampleCountX,
                        context.sampleCountY,
                        blendMinX + x,
                        blendMinY + y,
                        blendMinZ + z);

                    int cachedColor = slice.blockColors[sliceIndex];

                    if (cachedColor == INVALID_COLOR)
                    {
                        int sampleX = sampleMinX + x;
                        int sampleY = sampleMinY + y;
                        int sampleZ = sampleMinZ + z;

                        int sampleBlockX = blendConfig.getBlockFromSample(sampleX);
                        int sampleBlockY = blendConfig.getBlockFromSample(sampleY);
                        int sampleBlockZ = blendConfig.getBlockFromSample(sampleZ);

                        int blockX = getRandomSamplePosition(sampleBlockX, blendConfig.sampleSizeLog2, SAMPLE_SEED_X);
                        int blockY = getRandomSamplePosition(sampleBlockY, blendConfig.sampleSizeLog2, SAMPLE_SEED_Y);
                        int blockZ = getRandomSamplePosition(sampleBlockZ, blendConfig.sampleSizeLog2, SAMPLE_SEED_Z);

                        blockPos.set(blockX, blockY, blockZ);

                        cachedColor = getColorForBlock(world, blockPos, blockX, blockZ, resolver);

                        slice.blockColors[sliceIndex] = cachedColor;
                    }

                    context.setColorSample(cachedColor, blendIndex);
                }
            }
        }
    }

    public static void
    gatherColors(
        Level           world,
        ColorResolver   colorResolver,
        int             colorType,
        SliceCache      cache,
        ColorGenContext context)
    {
        for (int sliceZ = context.sliceMinZ;
             sliceZ < context.sliceMaxZ;
             ++sliceZ)
        {
            for (int sliceY = context.sliceMinY;
                 sliceY < context.sliceMaxY;
                 ++sliceY)
            {
                for (int sliceX = context.sliceMinX;
                     sliceX < context.sliceMaxX;
                     ++sliceX)
                {
                    Slice slice = cache.getSlice(
                        sliceX,
                        sliceY,
                        sliceZ,
                        colorType,
                        true,
                        false);

                    gatherColorsInSlice(
                        world,
                        colorResolver,
                        context,
                        slice,
                        sliceX,
                        sliceY,
                        sliceZ);

                    cache.releaseSlice(slice);
                }
            }
        }
    }
}
