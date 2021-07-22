package fionathemortal.betterbiomeblend;

import net.minecraft.world.World;
import net.minecraft.world.level.ColorResolver;

public final class ColorCaching
{
    public static long
    getChunkKey(int chunkX, int chunkZ, int colorType)
    {
        long result =
            ((long)(chunkZ & 0x7FFFFFFFL) << 31) |
            ((long)(chunkX & 0x7FFFFFFFL))       |
            ((long)colorType << 62);

        return result;
    }

    public static ColorChunk
    getThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, int chunkX, int chunkZ, int colorType)
    {
        ColorChunk result = null;
        ColorChunk local = threadLocal.get();

        long key = getChunkKey(chunkX, chunkZ, colorType);

        if (local.key == key)
        {
            result = local;
        }

        return result;
    }

    public static void
    setThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, ColorChunk chunk, ColorCache cache)
    {
        ColorChunk local = threadLocal.get();

        cache.releaseChunk(local);

        threadLocal.set(chunk);
    }

    public static ColorChunk
    getBlendedColorChunk(
        World         world,
        ColorResolver colorResolverIn,
        int           colorType,
        int           chunkX,
        int           chunkZ,
        ColorCache    cache,
        ColorCache    rawCache,
        BiomeCache    biomeCache)
    {
        ColorChunk chunk = cache.getChunk(chunkX, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = cache.newChunk(chunkX, chunkZ, colorType);

            ColorBlending.generateBlendedColorChunk(world, colorResolverIn, colorType, chunkX, chunkZ, rawCache, biomeCache, chunk.data);

            cache.putChunk(chunk);
        }

        return chunk;
    }
}
