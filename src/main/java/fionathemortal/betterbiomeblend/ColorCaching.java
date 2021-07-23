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
    setThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, ColorChunk chunk, BlendCache cache)
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
        BlendCache    blendCache,
        ColorCache    colorCache,
        BiomeCache    biomeCache)
    {
        ColorChunk chunk = blendCache.getChunk(chunkX, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = blendCache.newChunk(chunkX, chunkZ, colorType);

            ColorBlending.generateBlendedColorChunk(world, colorResolverIn, colorType, chunkX, chunkZ, colorCache, biomeCache, chunk.data);

            blendCache.putChunk(chunk);
        }

        return chunk;
    }
}
