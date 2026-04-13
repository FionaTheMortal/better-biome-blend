package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import fionathemortal.betterbiomeblend.common.util.*;

import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;

public final class ColorGen
{
    private static final ThreadLocal<ColorGenContext> threadLocalContext = new ThreadLocal<>();

    private static ColorGenContext
    acquireContext(ColorConfig config)
    {
        ColorGenContext result = threadLocalContext.get();

        if (result == null || result.blendConfig != config)
        {
            result = new ColorGenContext(config);
        }

        return result;
    }

    public static void
    endChunkGen(ColorGenContext context)
    {
        threadLocalContext.set(context);
    }

    public static ColorGenContext
    gatherColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorConfig   config,
        SliceCache    sourceCache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int           blockDimX,
        int           blockDimY,
        int           blockDimZ)
    {
        ColorGenContext colorGenContext = acquireContext(config);

        colorGenContext.initSource(
            blockMinX,
            blockMinY,
            blockMinZ,
            blockDimX,
            blockDimY,
            blockDimZ);

        ColorGather.gatherColors(
            world,
            colorResolver,
            colorType,
            sourceCache,
            colorGenContext);

        return colorGenContext;
    }

    public static ColorGenContext
    gatherColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorConfig   config,
        SliceCache    cache,
        int           chunkX,
        int           chunkY,
        int           chunkZ)
    {
        int blockMinX = Util.chunkToBlock(chunkX);
        int blockMinY = Util.chunkToBlock(chunkY);
        int blockMinZ = Util.chunkToBlock(chunkZ);

        int blockDimX = Util.CHUNK_SIZE;
        int blockDimY = Util.CHUNK_SIZE;
        int blockDimZ = Util.CHUNK_SIZE;

        ColorGenContext result = gatherColors(
            world,
            colorResolver,
            colorType,
            config,
            cache,
            blockMinX,
            blockMinY,
            blockMinZ,
            blockDimX,
            blockDimY,
            blockDimZ);

        return result;
    }

    private static void
    outputSingleColor(ColorGenContext colorGenContext)
    {
        int color = colorGenContext.colorBitsExclusive;

        Array3i.fill(
            color,
            colorGenContext.output,
            colorGenContext.outputArrayDimX,
            colorGenContext.outputArrayDimY,
            colorGenContext.outputMinX,
            colorGenContext.outputMinY,
            colorGenContext.outputMinZ,
            colorGenContext.outputDimX,
            colorGenContext.outputDimY,
            colorGenContext.outputDimZ);
    }

    private static void
    outputColorsDirectly(ColorGenContext context)
    {
        Array3i.copy(
            context.samples,
            context.sampleCountX,
            context.sampleCountY,
            0,
            0,
            0,
            context.output,
            context.outputArrayDimX,
            context.outputArrayDimY,
            context.outputMinX,
            context.outputMinY,
            context.outputMinZ,
            context.outputDimX,
            context.outputDimY,
            context.outputDimZ);
    }

    public static void
    blendColors(
        ColorGenContext context,
        int[]           output,
        int             outputMinX,
        int             outputMinY,
        int             outputMinZ,
        int             outputArrayDimX,
        int             outputArrayDimY)
    {
        context.initOutput(
            output,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputArrayDimX,
            outputArrayDimY);

        if (context.isSolid())
        {
            outputSingleColor(context);
        }
        else
        {
            if (context.blendConfig.blendRadius != 0)
            {
                ColorBlending.blendColors(context);
            }
            else
            {
                outputColorsDirectly(context);
            }
        }

        context.releaseOutput();
    }
}
