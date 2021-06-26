package fionathemortal.betterbiomeblend;

import fionathemortal.betterbiomeblend.mixin.AccessorChunkCache;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import scala.tools.cmd.Opt;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class BiomeColor
{
    public static final Lock                   freeBlendCacheslock = new ReentrantLock();
    public static final Stack<ColorBlendCache> freeBlendCaches     = new Stack<>();

    public static final byte[]
    neighbourOffsets =
    {
        -1, -1,
         0, -1,
         1, -1,
        -1,  0,
         0,  0,
         1,  0,
        -1,  1,
         0,  1,
         1,  1
    };

    public static final byte[]
    neighbourRectParams =
    {
        -1, -1,  0,  0, -16, -16,  0,  0,
         0, -1,  0,  0,   0, -16,  0,  0,
         0, -1, -1,  0,  16, -16,  0,  0,
        -1,  0,  0,  0, -16,   0,  0,  0,
         0,  0,  0,  0,   0,   0,  0,  0,
         0,  0, -1,  0,  16,   0,  0,  0,
        -1,  0,  0, -1, -16,  16,  0,  0,
         0,  0,  0, -1,   0,  16,  0,  0,
         0,  0, -1, -1,  16,  16,  0,  0
    };

    public static int
    getNeighbourOffsetX(int chunkIndex)
    {
        int result = neighbourOffsets[2 * chunkIndex + 0];

        return result;
    }

    public static int
    getNeighbourOffsetZ(int chunkIndex)
    {
        int result = neighbourOffsets[2 * chunkIndex + 1];

        return result;
    }

    public static int
    getNeighbourRectMinX(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = neighbourRectParams[offset + 0] & (16 - radius);

        return result;
    }

    public static int
    getNeighbourRectMinZ(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = neighbourRectParams[offset + 1] & (16 - radius);

        return result;
    }

    public static int
    getNeighbourRectMaxX(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = (neighbourRectParams[offset + 2] & (radius - 16)) + 16;

        return result;
    }

    public static int
    getNeighbourRectMaxZ(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = (neighbourRectParams[offset + 3] & (radius - 16)) + 16;

        return result;
    }

    public static int
    getNeighbourRectBlendCacheMinX(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = Math.max(neighbourRectParams[offset + 4] + radius, 0);

        return result;
    }

    public static int
    getNeighbourRectBlendCacheMinZ(int chunkIndex, int radius)
    {
        int offset = 8 * chunkIndex;
        int result = Math.max(neighbourRectParams[offset + 5] + radius, 0);

        return result;
    }

    public static void
    clearBlendCaches()
    {
        freeBlendCacheslock.lock();

        freeBlendCaches.clear();

        freeBlendCacheslock.unlock();
    }

    public static ColorBlendCache
    acquireBlendCache(int blendRadius)
    {
        ColorBlendCache result = null;

        freeBlendCacheslock.lock();

        while (!freeBlendCaches.empty())
        {
            result = freeBlendCaches.pop();

            if (result.blendRadius == blendRadius)
            {
                break;
            }
        }

        freeBlendCacheslock.unlock();

        if (result == null)
        {
            result = new ColorBlendCache(blendRadius);
        }

        return result;
    }

    public static void
    releaseBlendCache(ColorBlendCache cache)
    {
        freeBlendCacheslock.lock();

        int blendRadius = BetterBiomeBlendConfig.blendRadius;

        if (cache.blendRadius == blendRadius)
        {
            freeBlendCaches.push(cache);
        }

        freeBlendCacheslock.unlock();
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalGrassChunkWrapper(IBlockAccess blockAccess)
    {
        ThreadLocal<ColorChunk> threadLocal = null;

        World world = getWorldFromBlockAccess(blockAccess);

        if (world instanceof ColorChunkCacheProvider)
        {
            threadLocal = ((ColorChunkCacheProvider)world).getTreadLocalGrassChunk();
        }
        else
        {
            threadLocal = StaticCompatibilityCache.getThreadLocalGrassChunkWrapper();
        }

        return threadLocal;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalWaterChunkWrapper(IBlockAccess blockAccess)
    {
        ThreadLocal<ColorChunk> threadLocal = null;

        World world = getWorldFromBlockAccess(blockAccess);

        if (world instanceof ColorChunkCacheProvider)
        {
            threadLocal = ((ColorChunkCacheProvider)world).getTreadLocalWaterChunk();
        }
        else
        {
            threadLocal = StaticCompatibilityCache.getThreadLocalWaterChunkWrapper();
        }

        return threadLocal;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalFoliageChunkWrapper(IBlockAccess blockAccess)
    {
        ThreadLocal<ColorChunk> threadLocal = null;

        World world = getWorldFromBlockAccess(blockAccess);

        if (world instanceof ColorChunkCacheProvider)
        {
            threadLocal = ((ColorChunkCacheProvider)world).getTreadLocalFoliageChunk();
        }
        else
        {
            threadLocal = StaticCompatibilityCache.getThreadLocalFoliageChunkWrapper();
        }

        return threadLocal;
    }

    public static ThreadLocal<ColorChunk>
    getThreadLocalGenericChunkWrapper(IBlockAccess blockAccess)
    {
        ThreadLocal<ColorChunk> threadLocal = null;

        World world = getWorldFromBlockAccess(blockAccess);

        if (world instanceof ColorChunkCacheProvider)
        {
            threadLocal = ((ColorChunkCacheProvider)world).getTreadLocalGenericChunk();
        }
        else
        {
            threadLocal = StaticCompatibilityCache.getThreadLocalGenericChunkWrapper();
        }

        return threadLocal;
    }

    public static ColorChunk
    getThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, int chunkX, int chunkZ, int colorType)
    {
        ColorChunk result = null;
        ColorChunk local = threadLocal.get();

        long key = ColorChunkCache.getChunkKey(chunkX, chunkZ, colorType);

        if (local.key == key)
        {
            result = local;
        }

        return result;
    }

    public static void
    setThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, ColorChunk chunk, ColorChunkCache cache)
    {
        ColorChunk local = threadLocal.get();

        cache.releaseChunk(local);

        threadLocal.set(chunk);
    }

    public static void
    gatherRawColorsForChunk(
        IBlockAccess blockAccess,
        byte[]       result,
        int          chunkX,
        int          chunkZ,
        BiomeColorHelper.ColorResolver colorResolver)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int blockX = 16 * chunkX;
        int blockZ = 16 * chunkZ;

        int dstIndex = 0;

        for (int z = 0;
            z < 16;
            ++z)
        {
            for (int x = 0;
                x < 16;
                ++x)
            {
                blockPos.setPos(blockX + x, 0, blockZ + z);

                int color = colorResolver.getColorAtPos(blockAccess.getBiome(blockPos), blockPos);

                int colorR = Color.RGBAGetR(color);
                int colorG = Color.RGBAGetG(color);
                int colorB = Color.RGBAGetB(color);

                result[3 * dstIndex + 0] = (byte)colorR;
                result[3 * dstIndex + 1] = (byte)colorG;
                result[3 * dstIndex + 2] = (byte)colorB;

                ++dstIndex;
            }
        }
    }

    public static void
    gatherRawColorsToBlendCache(
        IBlockAccess blockAccess,
        int    chunkX,
        int    chunkZ,
        int    blendRadius,
        byte[] result,
        int    chunkIndex,
        BiomeColorHelper.ColorResolver colorResolver)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int blockX = chunkX << 4;
        int blockZ = chunkZ << 4;

        int srcMinX = getNeighbourRectMinX(chunkIndex, blendRadius);
        int srcMinZ = getNeighbourRectMinZ(chunkIndex, blendRadius);
        int srcMaxX = getNeighbourRectMaxX(chunkIndex, blendRadius);
        int srcMaxZ = getNeighbourRectMaxZ(chunkIndex, blendRadius);
        int dstMinX = getNeighbourRectBlendCacheMinX(chunkIndex, blendRadius);
        int dstMinZ = getNeighbourRectBlendCacheMinZ(chunkIndex, blendRadius);

        int dstDim  = 16 + 2 * blendRadius;
        int dstLine = 3 * (dstMinX + dstMinZ * dstDim);

        for (int z = srcMinZ;
            z < srcMaxZ;
            ++z)
        {
            int dstIndex = dstLine;

            for (int x = srcMinX;
                x < srcMaxX;
                ++x)
            {
                blockPos.setPos(blockX + x, 0, blockZ + z);

                int color = colorResolver.getColorAtPos(blockAccess.getBiome(blockPos), blockPos);

                int colorR = Color.RGBAGetR(color);
                int colorG = Color.RGBAGetG(color);
                int colorB = Color.RGBAGetB(color);

                result[dstIndex + 0] = (byte)colorR;
                result[dstIndex + 1] = (byte)colorG;
                result[dstIndex + 2] = (byte)colorB;

                dstIndex += 3;
            }

            dstLine += 3 * dstDim;
        }
    }

    public static void
    gatherRawColorsToBlendCache(
        IBlockAccess                   blockAccess,
        int                            chunkX,
        int                            chunkZ,
        int                            blendRadius,
        byte[]                         result,
        BiomeColorHelper.ColorResolver colorResolver)
    {
        for (int chunkIndex = 0;
            chunkIndex < 9;
            ++chunkIndex)
        {
            int offsetX = getNeighbourOffsetX(chunkIndex);
            int offsetZ = getNeighbourOffsetZ(chunkIndex);

            int rawChunkX = chunkX + offsetX;
            int rawChunkZ = chunkZ + offsetZ;

            gatherRawColorsToBlendCache(
                blockAccess,
                rawChunkX,
                rawChunkZ,
                blendRadius,
                result,
                chunkIndex,
                colorResolver);
        }
    }

    public static void
    blendCachedColorsForChunk(IBlockAccess blockAccess, byte[] result, ColorBlendCache blendCache)
    {
        int[] R = blendCache.R;
        int[] G = blendCache.G;
        int[] B = blendCache.B;

        int blendRadius = blendCache.blendRadius;
        int blendDim = 2 * blendRadius + 1;
        int blendCacheDim = 16 + 2 * blendRadius;
        int blendCount = blendDim * blendDim;

        for (int x = 0;
            x < blendCacheDim;
            ++x)
        {
            R[x] = 0xFF & blendCache.color[3 * x + 0];
            G[x] = 0xFF & blendCache.color[3 * x + 1];
            B[x] = 0xFF & blendCache.color[3 * x + 2];
        }

        for (int z = 1;
            z < blendDim;
            ++z)
        {
            for (int x = 0;
                x < blendCacheDim;
                ++x)
            {
                R[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 0];
                G[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 1];
                B[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 2];
            }
        }

        for (int z = 0;
            z < 16;
            ++z)
        {
            int accumulatedR = 0;
            int accumulatedG = 0;
            int accumulatedB = 0;

            for (int x = 0;
                x < blendDim;
                ++x)
            {
                accumulatedR += R[x];
                accumulatedG += G[x];
                accumulatedB += B[x];
            }

            for (int x = 0;
                x < 16;
                ++x)
            {
                int colorR = accumulatedR / blendCount;
                int colorG = accumulatedG / blendCount;
                int colorB = accumulatedB / blendCount;

                result[3 * (16 * z + x) + 0] = (byte)colorR;
                result[3 * (16 * z + x) + 1] = (byte)colorG;
                result[3 * (16 * z + x) + 2] = (byte)colorB;

                if (x < 15)
                {
                    accumulatedR += R[x + blendDim] - R[x];
                    accumulatedG += G[x + blendDim] - G[x];
                    accumulatedB += B[x + blendDim] - B[x];
                }
            }

            if (z < 15)
            {
                for (int x = 0;
                    x < blendCacheDim;
                    ++x)
                {
                    int index1 = 3 * (blendCacheDim * (z           ) + x);
                    int index2 = 3 * (blendCacheDim * (z + blendDim) + x);

                    R[x] += (0xFF & blendCache.color[index2 + 0]) - (0xFF & blendCache.color[index1 + 0]);
                    G[x] += (0xFF & blendCache.color[index2 + 1]) - (0xFF & blendCache.color[index1 + 1]);
                    B[x] += (0xFF & blendCache.color[index2 + 2]) - (0xFF & blendCache.color[index1 + 2]);
                }
            }
        }
    }

    public static void
    generateBlendedColorChunk(
        IBlockAccess                   blockAccess,
        int                            chunkX,
        int                            chunkZ,
        byte[]                         result,
        int                            colorType,
        BiomeColorHelper.ColorResolver colorResolver)
    {
        int blendRadius = BetterBiomeBlendConfig.blendRadius;

        if (blendRadius >  BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MIN &&
            blendRadius <= BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MAX)
        {
            ColorBlendCache blendCache = acquireBlendCache(blendRadius);

            gatherRawColorsToBlendCache(blockAccess, chunkX, chunkZ, blendCache.blendRadius, blendCache.color, colorResolver);

            blendCachedColorsForChunk(blockAccess, result, blendCache);

            releaseBlendCache(blendCache);
        }
        else
        {
            gatherRawColorsForChunk(blockAccess, result, chunkX, chunkZ, colorResolver);
        }
    }

    public static World
    getWorldFromBlockAccess(IBlockAccess blockAccess)
    {
        World result = null;

        if (blockAccess instanceof World)
        {
            result = (World)blockAccess;
        }
        else
        {
            if (blockAccess instanceof AccessorChunkCache)
            {
                AccessorChunkCache chunkCache = (AccessorChunkCache)blockAccess;

                result = chunkCache.getWorld();
            }
            else
            {
                if (OptifineCompatibility.isChunkCacheOF(blockAccess))
                {
                    ChunkCache chunkCache = OptifineCompatibility.getChunkCacheFromChunkCacheOF(blockAccess);

                    if (chunkCache instanceof AccessorChunkCache)
                    {
                        result = ((AccessorChunkCache)chunkCache).getWorld();
                    }
                }
            }
        }

        return result;
    }

    public static ColorChunkCache
    getColorChunkCacheForWorld(World world)
    {
        ColorChunkCache cache = null;

        if (world instanceof ColorChunkCacheProvider)
        {
            ColorChunkCacheProvider cacheProvider = (ColorChunkCacheProvider)world;

            cache = cacheProvider.getColorChunkCache();
        }
        else
        {
            cache = StaticCompatibilityCache.getColorChunkCache();
        }

        return cache;
    }

    public static ColorChunkCache
    getColorChunkCacheForIBlockAccess(IBlockAccess blockAccess)
    {
        World world = getWorldFromBlockAccess(blockAccess);

        ColorChunkCache cache = getColorChunkCacheForWorld(world);

        return cache;
    }

    public static ColorChunk
    getBlendedColorChunk(
        ColorChunkCache                cache,
        IBlockAccess                   blockAccess,
        int                            colorID,
        int                            chunkX,
        int                            chunkZ,
        BiomeColorHelper.ColorResolver colorResolver)
    {
        ColorChunk chunk = cache.getChunk(chunkX, chunkZ, colorID);

        if (chunk == null)
        {
            chunk = cache.newChunk(chunkX, chunkZ, colorID);

            generateBlendedColorChunk(blockAccess, chunkX, chunkZ, chunk.data, colorID, colorResolver);

            cache.putChunk(chunk);
        }

        return chunk;
    }
}
