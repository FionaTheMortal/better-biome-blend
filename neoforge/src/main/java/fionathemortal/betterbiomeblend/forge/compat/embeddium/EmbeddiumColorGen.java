package fionathemortal.betterbiomeblend.forge.compat.embeddium;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.ColorGen;
import fionathemortal.betterbiomeblend.common.ColorGenContext;
import net.minecraft.world.level.ColorResolver;
import org.embeddedt.embeddium.impl.world.biome.BiomeSlice;

public final class EmbeddiumColorGen
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
        BiomeSlice    source,
        ColorResolver resolver,
        ColorConfig   config,
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

        EmbeddiumColorGather.gatherColors(source, resolver, context);

        return context;
    }

    public static void
    genColors(
        BiomeSlice    source,
        ColorResolver resolver,
        ColorConfig   config,
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
            source,
            resolver,
            config,
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
