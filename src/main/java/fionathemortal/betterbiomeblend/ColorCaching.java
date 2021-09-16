package fionathemortal.betterbiomeblend;

import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;

public final class ColorCaching
{
    public static final int INVALID_CHUNK_KEY = -1;

    public static long
    getChunkKey(int chunkX, int chunkZ, int colorType)
    {
        long result =
            ((long)(chunkZ & 0x03FFFFFFL) << 26) |
            ((long)(chunkX & 0x03FFFFFFL))       |
            ((long)colorType << 52);

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
        Level world,
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

            chunk = blendCache.putChunk(chunk);
        }

        return chunk;
    }
}
