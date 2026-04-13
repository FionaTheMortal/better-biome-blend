package fionathemortal.betterbiomeblend.fabric.compat.sodium;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.ColorGather;
import fionathemortal.betterbiomeblend.common.ColorGen;
import fionathemortal.betterbiomeblend.common.ColorGenContext;
import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;

public final class SodiumColorGen
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
        ColorResolver resolver,
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
        ColorGenContext context = acquireContext(config);

        context.initSource(
            blockMinX,
            blockMinY,
            blockMinZ,
            blockDimX,
            blockDimY,
            blockDimZ);

        ColorGather.gatherColors(
            world,
            resolver,
            colorType,
            sourceCache,
            context);

        return context;
    }

    public static void
    genColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorConfig   config,
        SliceCache    cache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int           blockDimX,
        int           blockDimY,
        int           blockDimZ,
        int[]         output,
        int           outputMinX,
        int           outputMinY,
        int           outputMinZ,
        int           outputArrayDimX,
        int           outputArrayDimY)
    {
        ColorGenContext context = gatherColors(
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

        ColorGen.blendColors(
            context,
            output,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputArrayDimX,
            outputArrayDimY);

        endChunkGen(context);
    }
}
