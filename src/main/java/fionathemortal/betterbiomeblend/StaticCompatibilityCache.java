package fionathemortal.betterbiomeblend;

import net.minecraft.world.IBlockAccess;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StaticCompatibilityCache
{
    private static final ThreadLocal<ColorChunk> threadLocalGrassChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    private static final ThreadLocal<ColorChunk> threadLocalWaterChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    private static final ThreadLocal<ColorChunk> threadLocalFoliageChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    private static AtomicBoolean   isGenerated     = new AtomicBoolean(false);
    private static ColorChunkCache colorChunkCache = null;

    public static ColorChunkCache
    getColorChunkCache()
    {
        if (!isGenerated.get())
        {
            if (isGenerated.compareAndSet(false, true))
            {
                colorChunkCache = new ColorChunkCache(2048);
            }
            else
            {
                while (colorChunkCache == null)
                {
                }
            }
        }

        return colorChunkCache;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalGrassChunkWrapper()
    {
        return threadLocalGrassChunk;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalWaterChunkWrapper()
    {
        return threadLocalWaterChunk;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalFoliageChunkWrapper()
    {
        return threadLocalFoliageChunk;
    }
}
