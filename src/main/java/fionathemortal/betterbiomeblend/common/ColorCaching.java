package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.BiomeCache;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;

public final class ColorCaching
{
    public static final int INVALID_CHUNK_KEY = -1;

    public static long
    getChunkKey(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long result =
            ((long)(chunkZ & 0x03FFFFFFL) << 26) |
            ((long)(chunkX & 0x03FFFFFFL))       |
            ((long)(chunkY & 0x1F       ) << 52) |
            ((long)colorType << 57);

        return result;
    }

    public static BlendChunk
    getThreadLocalChunk(ThreadLocal<BlendChunk> threadLocal, int chunkX, int chunkY, int chunkZ, int colorType)
    {
        BlendChunk result = null;
        BlendChunk local = threadLocal.get();

        long key = getChunkKey(chunkX, chunkY, chunkZ, colorType);

        if (local.key == key)
        {
            result = local;
        }

        return result;
    }

    public static void
    setThreadLocalChunk(ThreadLocal<BlendChunk> threadLocal, BlendChunk chunk, BlendCache cache)
    {
        BlendChunk local = threadLocal.get();

        cache.releaseChunk(local);

        threadLocal.set(chunk);
    }

    public static BlendChunk
    getBlendedColorChunk(
        Level         world,
        ColorResolver colorResolverIn,
        int           colorType,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        BlendCache    blendCache,
        ColorCache    colorCache,
        BiomeCache    biomeCache)
    {
        BlendChunk chunk = blendCache.getChunk(chunkX, chunkY, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = blendCache.newChunk(chunkX, chunkY, chunkZ, colorType);

            ColorBlending.generateBlendedColorChunk(
                world,
                colorResolverIn,
                colorType,
                chunkX,
                chunkY,
                chunkZ,
                colorCache,
                biomeCache,
                chunk.data);

            chunk = blendCache.putChunk(chunk);
        }

        return chunk;
    }
}
