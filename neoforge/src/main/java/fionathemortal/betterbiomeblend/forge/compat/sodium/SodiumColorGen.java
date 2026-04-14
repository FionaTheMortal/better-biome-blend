package fionathemortal.betterbiomeblend.forge.compat.sodium;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.ColorGen;
import fionathemortal.betterbiomeblend.common.ColorGenContext;
import net.caffeinemc.mods.sodium.client.world.biome.LevelBiomeSlice;
import net.minecraft.world.level.ColorResolver;

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
        LevelBiomeSlice source,
        ColorResolver   resolver,
        ColorConfig     config,
        int             blockMinX,
        int             blockMinY,
        int             blockMinZ,
        int             blockDimX,
        int             blockDimY,
        int             blockDimZ)
    {
        ColorGenContext context = acquireContext(config);

        context.initSource(
            blockMinX,
            blockMinY,
            blockMinZ,
            blockDimX,
            blockDimY,
            blockDimZ);

        SodiumColorGather.gatherColors(source, resolver, context);

        return context;
    }

    public static void
    genColors(
        LevelBiomeSlice source,
        ColorResolver   resolver,
        ColorConfig     config,
        int             blockMinX,
        int             blockMinY,
        int             blockMinZ,
        int             blockDimX,
        int             blockDimY,
        int             blockDimZ,
        int[]           output,
        int             outputMinX,
        int             outputMinY,
        int             outputMinZ,
        int             outputArrayDimX,
        int             outputArrayDimY)
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
