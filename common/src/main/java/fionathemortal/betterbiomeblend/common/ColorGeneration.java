package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.cache.output.BlendChunk;
import fionathemortal.betterbiomeblend.common.cache.source.ColorCache;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import fionathemortal.betterbiomeblend.common.util.*;

import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;

public final class ColorGeneration
{
    private static final ThreadLocal<BlendContext> threadLocalBlendContext = new ThreadLocal<>();

    private static BlendContext
    acquireBlendContext()
    {
        BlendConfig config = BetterBiomeBlendClient.getCurrentBlendConfig();

        BlendContext result = threadLocalBlendContext.get();

        if (result == null || result.blendConfig != config)
        {
            result = new BlendContext(config);
        }

        return result;
    }

    private static void
    releaseBlendContext(BlendContext context)
    {
        context.free();

        threadLocalBlendContext.set(context);
    }

    private static void
    outputSingularColor(BlendContext blendContext)
    {
        int color = blendContext.colorBitsExclusive;

        Array3i.fill(
            color,
            blendContext.output,
            blendContext.outputArrayDimX,
            blendContext.outputArrayDimY,
            blendContext.outputMinX,
            blendContext.outputMinY,
            blendContext.outputMinZ,
            blendContext.outputDimX,
            blendContext.outputDimY,
            blendContext.outputDimZ);
    }

    public static void
    generateColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int[]         output,
        int           outputMinX,
        int           outputMinY,
        int           outputMinZ,
        int           outputDimX,
        int           outputDimY,
        int           outputDimZ,
        int           outputArrayDimX,
        int           outputArrayDimY)
    {
        DebugEvent debugEvent = Debug.pushColorGenEvent(blockMinX, blockMinY, blockMinZ, colorType);

        BlendContext blendContext = acquireBlendContext();

        blendContext.init(
            blockMinX,
            blockMinY,
            blockMinZ,
            output,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputDimX,
            outputDimY,
            outputDimZ,
            outputArrayDimX,
            outputArrayDimY);

        if (blendContext.blendConfig.blendRadius >  BlendConfig.BIOME_BLEND_RADIUS_MIN &&
            blendContext.blendConfig.blendRadius <= BlendConfig.BIOME_BLEND_RADIUS_MAX)
        {
            ColorGather.gatherColorsForBlending(
                world,
                colorResolver,
                colorType,
                colorCache,
                blendContext);

            if (!blendContext.isSingleColor())
            {
                DebugEvent subEvent = Debug.pushSubevent(DebugEventType.SUBEVENT);

                ColorBlending.blendColors(blendContext);

                Debug.endEvent(subEvent);
            }
            else
            {
                outputSingularColor(blendContext);
            }
        }
        else
        {
            // gatherColorsDirectly();
        }

        releaseBlendContext(blendContext);

        Debug.endEvent(debugEvent);
    }

    public static void
    generateColorsForChunk(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BlendChunk blendChunk,
        int           blockX,
        int           blockY,
        int           blockZ)
    {
        int chunkX = Utility.blockToChunk(blockX);
        int chunkY = Utility.blockToChunk(blockY);
        int chunkZ = Utility.blockToChunk(blockZ);

        int blockMinX = Utility.chunkToBlock(chunkX);
        int blockMinY = Utility.chunkToBlock(chunkY);
        int blockMinZ = Utility.chunkToBlock(chunkZ);

        generateColors(
            world,
            colorResolver,
            colorType,
            colorCache,
            blockMinX,
            blockMinY,
            blockMinZ,
            blendChunk.blockColors,
            0,
            0,
            0,
            16,
            16,
            16,
            16,
            16);
    }

    public static void
    finalizeColorGen(
        BlendContext blendContext,
        BlendChunk   blendChunk)
    {
        if (!blendContext.isSingleColor())
        {
            blendContext.output = blendChunk.blockColors;

            ColorBlending.blendColors(blendContext);
        }
        else
        {
            blendChunk.storesConstColor = true;
            blendChunk.chunkColor = blendContext.colorBitsExclusive;
        }

        releaseBlendContext(blendContext);
    }

    public static BlendContext
    initColorGen(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int           outputMinX,
        int           outputMinY,
        int           outputMinZ,
        int           outputDimX,
        int           outputDimY,
        int           outputDimZ,
        int           outputArrayDimX,
        int           outputArrayDimY)
    {
        BlendContext blendContext = acquireBlendContext();

        blendContext.init(
            blockMinX,
            blockMinY,
            blockMinZ,
            null,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputDimX,
            outputDimY,
            outputDimZ,
            outputArrayDimX,
            outputArrayDimY);

        if (blendContext.blendConfig.blendRadius >  BlendConfig.BIOME_BLEND_RADIUS_MIN &&
            blendContext.blendConfig.blendRadius <= BlendConfig.BIOME_BLEND_RADIUS_MAX)
        {
            ColorGather.gatherColorsForBlending(
                world,
                colorResolver,
                colorType,
                colorCache,
                blendContext);
        }
        else
        {
        }

        return blendContext;
    }

    public static BlendContext
    initColorGenForChunk(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        int           blockX,
        int           blockY,
        int           blockZ)
    {
        int chunkX = Utility.blockToChunk(blockX);
        int chunkY = Utility.blockToChunk(blockY);
        int chunkZ = Utility.blockToChunk(blockZ);

        int blockMinX = Utility.chunkToBlock(chunkX);
        int blockMinY = Utility.chunkToBlock(chunkY);
        int blockMinZ = Utility.chunkToBlock(chunkZ);

        return initColorGen(
            world,
            colorResolver,
            colorType,
            colorCache,
            blockMinX,
            blockMinY,
            blockMinZ,
            0,
            0,
            0,
            16,
            16,
            16,
            16,
            16);
    }
}
