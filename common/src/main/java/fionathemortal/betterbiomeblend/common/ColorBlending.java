package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.cache.BiomeCache;
import fionathemortal.betterbiomeblend.common.cache.BiomeSlice;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.ColorSlice;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class ColorBlending
{
    public static final ReentrantLock           freeBlendBuffersLock = new ReentrantLock();
    public static final Stack<BlendBuffer> freeBlendBuffers     = new Stack<>();

    public static int
    getSliceMin(int blendRadius, int blockSizeLog2, int sliceSizeLog2, int sliceIndex)
    {
        final int sliceSize       = 1 << sliceSizeLog2;
        final int scaledSliceSize = sliceSize   >> blockSizeLog2;

        final int scaledBlendDiameter    = (2 * blendRadius) >> blockSizeLog2;
        final int scaledLowerBlendRadius = scaledBlendDiameter - (scaledBlendDiameter >> 1);

        int result = 0;

        if (sliceIndex == -1)
        {
            result = scaledSliceSize - scaledLowerBlendRadius;
        }

        return result;
    }

    public static int
    getSliceMax(int blendRadius, int blockSizeLog2, int sliceSizeLog2, int sliceIndex)
    {
        final int sliceSize       = 1 << sliceSizeLog2;
        final int scaledSliceSize = sliceSize   >> blockSizeLog2;

        final int scaledBlendDiameter    = (2 * blendRadius) >> blockSizeLog2;
        final int scaledUpperBlendRadius = scaledBlendDiameter >> 1;

        int result = scaledSliceSize;

        if (sliceIndex == 1)
        {
            result = scaledUpperBlendRadius;
        }

        return result;
    }

    public static int
    getBlendMin(int blendRadius, int blockSizeLog2, int sliceSizeLog2, int sliceIndex)
    {
        final int sliceSize       = 1 << sliceSizeLog2;
        final int scaledSliceSize = sliceSize   >> blockSizeLog2;

        final int scaledBlendDiameter    = (2 * blendRadius) >> blockSizeLog2;
        final int scaledLowerBlendRadius = scaledBlendDiameter - (scaledBlendDiameter >> 1);

        int result = 0;

        if (sliceIndex >= 0)
        {
            result += scaledLowerBlendRadius;

            if (sliceIndex == 1)
            {
                result += scaledSliceSize;
            }
        }

        return result;
    }

    public static BlendBuffer
    acquireBlendBuffer(int blendRadius)
    {
        BlendBuffer result = null;

        freeBlendBuffersLock.lock();

        while (!freeBlendBuffers.empty())
        {
            BlendBuffer buffer = freeBlendBuffers.pop();

            if (buffer.blendRadius == blendRadius)
            {
                result = buffer;
                break;
            }
        }

        freeBlendBuffersLock.unlock();

        if (result == null)
        {
            result = new BlendBuffer(blendRadius);
        }

        return result;
    }

    public static void
    releaseBlendBuffer(BlendBuffer cache)
    {
        freeBlendBuffersLock.lock();

        freeBlendBuffers.push(cache);

        freeBlendBuffersLock.unlock();
    }

    public static Biome
    getDefaultBiome(Level world)
    {
        Biome result = null;

        Holder<Biome> biomeHolder = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(Biomes.PLAINS);

        if (biomeHolder.isBound())
        {
            result = biomeHolder.value();
        }

        return result;
    }

    public static Biome
    getBiomeAtPositionOrDefault(Level world, BlockPos blockPosition)
    {
        Biome result = null;

        Holder<Biome> biomeHolder = world.getBiome(blockPosition);

        if (biomeHolder.isBound())
        {
            result = biomeHolder.value();
        }
        else
        {
            result = getDefaultBiome(world);
        }

        return result;
    }

    public static Biome
    getBiomeAtPositionOrDefaultOrThrow(Level world, BlockPos blockPos)
    {
        Biome result = getBiomeAtPositionOrDefault(world, blockPos);

        if (result == null)
        {
            throw new IllegalStateException("Biome could not be retrieved for block position.");
        }

        return result;
    }

    static final AtomicLong colorCacheHitCount = new AtomicLong();
    static final AtomicLong biomeCacheHitCount = new AtomicLong();

    static final AtomicLong colorCacheMissCount = new AtomicLong();
    static final AtomicLong biomeCacheMissCount = new AtomicLong();

    public static void
    gatherColorsForSlice(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        BlendBuffer   blendBuffer,
        int           blendRadius,
        int           sliceIDX,
        int           sliceIDY,
        int           sliceIDZ,
        int           sliceX,
        int           sliceY,
        int           sliceZ)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        // TODO: Cleanup parameter code

        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        final int blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        final int sliceSize = 1 << sliceSizeLog2;

        final int blendSize = BlendConfig.getBlendBufferSize(blendRadius);

        final int sliceMinX = getSliceMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDX);
        final int sliceMinY = getSliceMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDY);
        final int sliceMinZ = getSliceMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDZ);

        final int sliceMaxX = getSliceMax(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDX);
        final int sliceMaxY = getSliceMax(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDY);
        final int sliceMaxZ = getSliceMax(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDZ);

        final int blendMinX = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDX);
        final int blendMinY = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDY);
        final int blendMinZ = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDZ);

        final int dimX = sliceMaxX - sliceMinX;
        final int dimY = sliceMaxY - sliceMinY;
        final int dimZ = sliceMaxZ - sliceMinZ;

        int worldMinX = (sliceX << sliceSizeLog2) + (sliceMinX << blockSizeLog2);
        int worldMinY = (sliceY << sliceSizeLog2) + (sliceMinY << blockSizeLog2);
        int worldMinZ = (sliceZ << sliceSizeLog2) + (sliceMinZ << blockSizeLog2);

        final int scaledBlendDiameter = (2 * blendRadius) >> blockSizeLog2;

        if ((scaledBlendDiameter & 1) != 0)
        {
            worldMinX += blockSizeLog2;
            worldMinY += blockSizeLog2;
            worldMinZ += blockSizeLog2;
        }

        // BiomeSlice biomeSlice = biomeCache.getOrDefaultInitializeSlice(blendRadius, sliceX, sliceY, sliceZ, 0);
        ColorSlice colorSlice = colorCache.getOrDefaultInitializeSlice(blendRadius, sliceX, sliceY, sliceZ, colorType);

        DebugEvent subEvent = Debug.pushSubevent(DebugEventType.SUBEVENT);

        for (int y = 0;
             y < dimY;
             ++y)
        {
            for (int z = 0;
                 z < dimZ;
                 ++z)
            {
                final int sliceIndexX = sliceMinX;
                final int sliceIndexY = sliceMinY + y;
                final int sliceIndexZ = sliceMinZ + z;

                final int blendIndexX = blendMinX;
                final int blendIndexY = blendMinY + y;
                final int blendIndexZ = blendMinZ + z;

                int sliceIndex = ColorCaching.getCacheArrayIndex(sliceSize, sliceIndexX, sliceIndexY, sliceIndexZ);
                int blendIndex = ColorCaching.getCacheArrayIndex(blendSize, blendIndexX, blendIndexY, blendIndexZ);

                int biomeSliceIndex = sliceIndex;

                sliceIndex *= 3;
                blendIndex *= 3;

                for (int x = 0;
                     x < dimX;
                     ++x)
                {
                    int cachedR = 0xFF & colorSlice.data[sliceIndex + 0];
                    int cachedG = 0xFF & colorSlice.data[sliceIndex + 1];
                    int cachedB = 0xFF & colorSlice.data[sliceIndex + 2];

                    final int commonBits = cachedR & cachedG & cachedB;

                    // NOTE: White is uninitialized data in vanilla code

                    if (commonBits == 0xFF)
                    {
                        // colorCacheMissCount.getAndIncrement();

                        Biome biome = null; // biomeSlice.data[biomeSliceIndex];

                        final int sampleMinX = worldMinX + (x << blockSizeLog2);
                        final int sampleMinY = worldMinY + (y << blockSizeLog2);
                        final int sampleMinZ = worldMinZ + (z << blockSizeLog2);

                        // TODO: Random sampling

                        final int sampleX = sampleMinX;
                        final int sampleY = sampleMinY;
                        final int sampleZ = sampleMinZ;

                        if (biome == null)
                        {
                            // biomeCacheMissCount.getAndIncrement();

                            blockPos.set(sampleX, sampleY, sampleZ);

                            biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

                            // biomeSlice.data[biomeSliceIndex] = biome;
                        }
                        else
                        {
                            // biomeCacheHitCount.getAndIncrement();
                        }

                        final double sampleXF64 = sampleX;
                        final double sampleZF64 = sampleZ;

                        final int color = colorResolver.getColor(biome, sampleXF64, sampleZF64);

                        cachedR = Color.RGBAGetR(color);
                        cachedG = Color.RGBAGetG(color);
                        cachedB = Color.RGBAGetB(color);

                        colorSlice.data[sliceIndex    ] = (byte)cachedR;
                        colorSlice.data[sliceIndex + 1] = (byte)cachedG;
                        colorSlice.data[sliceIndex + 2] = (byte)cachedB;
                    }
                    else
                    {
                        // colorCacheHitCount.getAndIncrement();
                    }

                    Color.sRGBByteToOKLabs(cachedR, cachedG, cachedB, blendBuffer.colors, blendIndex);

                    sliceIndex += 3;
                    blendIndex += 3;

                    ++biomeSliceIndex;
                }
            }
        }

        Debug.endEvent(subEvent);

        // double biomeRatio = ((double)biomeCacheHitCount.get() / (double)(biomeCacheHitCount.get() + biomeCacheMissCount.get()));
        // double colorRatio = ((double)colorCacheHitCount.get() / (double)(colorCacheHitCount.get() + colorCacheMissCount.get()));

        colorCache.releaseSlice(colorSlice);
        // biomeCache.releaseSlice(biomeSlice);
    }

    public static void
    gatherColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        BlendBuffer   blendBuffer,
        int           x,
        int           y,
        int           z,
        int           blendRadius)
    {
        int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);

        final int sliceX = x >> sliceSizeLog2;
        final int sliceY = y >> sliceSizeLog2;
        final int sliceZ = z >> sliceSizeLog2;

        ChunkAccess[] neighbors          = new ChunkAccess[9];
        boolean       neighborsAreLoaded = true;

        int neighborIndex = 0;

        for (int sliceIndexZ = -1;
             sliceIndexZ <= 1;
             ++sliceIndexZ)
        {
            for (int sliceIndexX = -1;
                 sliceIndexX <= 1;
                 ++sliceIndexX)
            {
                int neighborSliceX = sliceX + sliceIndexX;
                int neighborSliceZ = sliceZ + sliceIndexZ;

                int neighborChunkX = neighborSliceX >> (4 - sliceSizeLog2);
                int neighborChunkZ = neighborSliceZ >> (4 - sliceSizeLog2);

                ChunkAccess chunk = world.getChunk(neighborChunkX, neighborChunkZ, ChunkStatus.BIOMES, false);

                if (chunk == null)
                {
                    neighborsAreLoaded = false;
                }

                neighbors[neighborIndex] = chunk;

                ++neighborIndex;
            }
        }

        // TODO: Handle color gather if not all chunks are loaded

        if (!neighborsAreLoaded)
        {
            Arrays.fill(blendBuffer.colors, 0.0f);
        }
        else
        {
            neighborIndex = 0;

            for (int sliceIndexZ = -1;
                 sliceIndexZ <= 1;
                 ++sliceIndexZ)
            {
                for (int sliceIndexX = -1;
                     sliceIndexX <= 1;
                     ++sliceIndexX)
                {
                    for (int sliceIndexY = -1;
                         sliceIndexY <= 1;
                         ++sliceIndexY)
                    {
                        final int neighborSliceX = sliceX + sliceIndexX;
                        final int neighborSliceY = sliceY + sliceIndexY;
                        final int neighborSliceZ = sliceZ + sliceIndexZ;

                        if (neighbors[neighborIndex] != null)
                        {
                            gatherColorsForSlice(
                                world,
                                colorResolver,
                                colorType,
                                colorCache,
                                biomeCache,
                                blendBuffer,
                                blendRadius,
                                sliceIndexX,
                                sliceIndexY,
                                sliceIndexZ,
                                neighborSliceX,
                                neighborSliceY,
                                neighborSliceZ);
                        }
                    }

                    ++neighborIndex;
                }
            }
        }
    }

    public static void
    blendColorsForChunk(int blendRadius, BlendBuffer buffer, int[] result, int base)
    {
        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        final int blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        final int sliceSize = 1 << sliceSizeLog2;
        final int blockSize = 1 << blockSizeLog2;

        final int blendSize       = BlendConfig.getBlendSize(blendRadius);
        final int blendBufferSize = BlendConfig.getBlendBufferSize(blendRadius);

        final int scaledSliceSize = sliceSize >> blockSizeLog2;
        final int filterSupport   = BlendConfig.getFilterSupport(blendRadius);

        float[] colors   = buffer.colors;
        float[] scanline = buffer.scanline;

        final float factor = (1.0f / blockSize);

        for (int y = 0;
             y < blendSize;
             ++y)
        {
            for (int z = 0;
                 z < blendSize;
                 ++z)
            {
                int srcIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, 0, y, z);
                int dstIndex = 0;

                for (int x = 0;
                    x < blendSize;
                    ++x)
                {
                    scanline[dstIndex    ] = colors[srcIndex    ];
                    scanline[dstIndex + 1] = colors[srcIndex + 1];
                    scanline[dstIndex + 2] = colors[srcIndex + 2];

                    dstIndex += 3;
                    srcIndex += 3;
                }

                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                srcIndex = 0;

                for (int x = 0;
                     x < filterSupport - 1;
                     ++x)
                {
                    accumulatedR += scanline[srcIndex    ];
                    accumulatedG += scanline[srcIndex + 1];
                    accumulatedB += scanline[srcIndex + 2];

                    srcIndex += 3;
                }

                dstIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, 0, y, z);;

                int lower = 0;
                int upper = 3 * (filterSupport - 1);

                for (int x = 0;
                     x < scaledSliceSize;
                     ++x)
                {
                    float lowerR = scanline[lower    ] * factor;
                    float lowerG = scanline[lower + 1] * factor;
                    float lowerB = scanline[lower + 2] * factor;

                    float upperR = scanline[upper    ] * factor;
                    float upperG = scanline[upper + 1] * factor;
                    float upperB = scanline[upper + 2] * factor;

                    for (int i = 0;
                         i < blockSize;
                         ++i)
                    {
                        accumulatedR += upperR;
                        accumulatedG += upperG;
                        accumulatedB += upperB;

                        colors[dstIndex    ] = accumulatedR;
                        colors[dstIndex + 1] = accumulatedG;
                        colors[dstIndex + 2] = accumulatedB;

                        accumulatedR -= lowerR;
                        accumulatedG -= lowerG;
                        accumulatedB -= lowerB;

                        dstIndex += 3;
                    }

                    lower += 3;
                    upper += 3;
                }
            }
        }

        for (int y = 0;
             y < blendSize;
             ++y)
        {
            for (int x = 0;
                 x < sliceSize;
                 ++x)
            {
                final int stride = 3 * blendBufferSize;

                int srcIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, x, y, 0);
                int dstIndex = 0;

                for (int i = 0;
                     i < blendSize;
                     ++i)
                {
                    scanline[dstIndex    ] = colors[srcIndex    ];
                    scanline[dstIndex + 1] = colors[srcIndex + 1];
                    scanline[dstIndex + 2] = colors[srcIndex + 2];

                    dstIndex += 3;
                    srcIndex += stride;
                }

                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                srcIndex = 0;

                for (int z = 0;
                     z < filterSupport - 1;
                     ++z)
                {
                    accumulatedR += scanline[srcIndex    ];
                    accumulatedG += scanline[srcIndex + 1];
                    accumulatedB += scanline[srcIndex + 2];

                    srcIndex += 3;
                }

                dstIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, x, y, 0);;

                int lower = 0;
                int upper = 3 * (filterSupport - 1);

                for (int z = 0;
                     z < scaledSliceSize;
                     ++z)
                {
                    float lowerR = scanline[lower    ] * factor;
                    float lowerG = scanline[lower + 1] * factor;
                    float lowerB = scanline[lower + 2] * factor;

                    float upperR = scanline[upper    ] * factor;
                    float upperG = scanline[upper + 1] * factor;
                    float upperB = scanline[upper + 2] * factor;

                    for (int i = 0;
                         i < blockSize;
                         ++i)
                    {
                        accumulatedR += upperR;
                        accumulatedG += upperG;
                        accumulatedB += upperB;

                        colors[dstIndex    ] = accumulatedR;
                        colors[dstIndex + 1] = accumulatedG;
                        colors[dstIndex + 2] = accumulatedB;

                        accumulatedR -= lowerR;
                        accumulatedG -= lowerG;
                        accumulatedB -= lowerB;

                        dstIndex += stride;
                    }

                    lower += 3;
                    upper += 3;
                }
            }
        }

        float filter       = (float)(filterSupport - 1) + factor;
        float filterScalar = 1.0f / (filter * filter * filter);

        for (int z = 0;
             z < sliceSize;
             ++z)
        {
            for (int x = 0;
                 x < sliceSize;
                 ++x)
            {
                final int stride = 3 * blendBufferSize * blendBufferSize;

                int srcIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, x, 0, z);
                int dstIndex = 0;

                for (int i = 0;
                     i < blendSize;
                     ++i)
                {
                    scanline[dstIndex    ] = colors[srcIndex    ];
                    scanline[dstIndex + 1] = colors[srcIndex + 1];
                    scanline[dstIndex + 2] = colors[srcIndex + 2];

                    dstIndex += 3;
                    srcIndex += stride;
                }

                srcIndex = 0;

                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                for (int y = 0;
                     y < filterSupport - 1;
                     ++y)
                {
                    accumulatedR += scanline[srcIndex    ];
                    accumulatedG += scanline[srcIndex + 1];
                    accumulatedB += scanline[srcIndex + 2];

                    srcIndex += 3;
                }

                int lower = 0;
                int upper = 3 * (filterSupport - 1);

                dstIndex = base + ColorCaching.getCacheArrayIndex(16, x, 0, z);;

                for (int y = 0;
                     y < scaledSliceSize;
                     ++y)
                {
                    float lowerR = scanline[lower    ] * factor;
                    float lowerG = scanline[lower + 1] * factor;
                    float lowerB = scanline[lower + 2] * factor;

                    float upperR = scanline[upper    ] * factor;
                    float upperG = scanline[upper + 1] * factor;
                    float upperB = scanline[upper + 2] * factor;

                    for (int i = 0;
                         i < blockSize;
                         ++i)
                    {
                        accumulatedR += upperR;
                        accumulatedG += upperG;
                        accumulatedB += upperB;

                        float finalR = accumulatedR * filterScalar;
                        float finalG = accumulatedG * filterScalar;
                        float finalB = accumulatedB * filterScalar;

                        Color.OKLabsTosRGBAInt(finalR, finalG, finalB, result, dstIndex);

                        accumulatedR -= lowerR;
                        accumulatedG -= lowerG;
                        accumulatedB -= lowerB;

                        dstIndex += 16 * 16;
                    }

                    lower += 3;
                    upper += 3;
                }
            }
        }
    }

    public static void
    gatherColorsDirectly(
        Level         world,
        ColorResolver colorResolver,
        int           requestX,
        int           requestY,
        int           requestZ,
        int[]         result)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(0);
        final int sliceSize     = BlendConfig.getSliceSize(0);

        int sliceX = requestX >> sliceSizeLog2;
        int sliceY = requestY >> sliceSizeLog2;
        int sliceZ = requestZ >> sliceSizeLog2;

        int baseX = sliceX << sliceSizeLog2;
        int baseY = sliceY << sliceSizeLog2;
        int baseZ = sliceZ << sliceSizeLog2;

        final int inChunkX = Utility.getLowerBits(baseX, 4);
        final int inChunkY = Utility.getLowerBits(baseY, 4);
        final int inChunkZ = Utility.getLowerBits(baseZ, 4);

        final int base = ColorCaching.getCacheArrayIndex(16, inChunkX, inChunkY, inChunkZ);

        for (int y = 0;
            y < sliceSize;
            ++y)
        {
            for (int z = 0;
                 z < sliceSize;
                 ++z)
            {
                for (int x = 0;
                     x < sliceSize;
                     ++x)
                {
                    int worldX = baseX + x;
                    int worldY = baseY + y;
                    int worldZ = baseZ + z;

                    double worldXF64 = (double)worldX;
                    double worldZF64 = (double)worldZ;

                    blockPos.set(worldX, worldY, worldZ);

                    Biome biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

                    final int color = colorResolver.getColor(biome, worldXF64, worldZF64);

                    int index = base + ColorCaching.getCacheArrayIndex(16, x, y, z);

                    result[index] = color;
                }
            }
        }
    }

    public static void
    generateColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        int           x,
        int           y,
        int           z,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        int[]         result)
    {
        DebugEvent debugEvent = Debug.pushColorGenEvent(x, y, z, colorType);

        final int blendRadius = BetterBiomeBlendClient.getBiomeBlendRadius();

        if (blendRadius >  BlendConfig.BIOME_BLEND_RADIUS_MIN &&
            blendRadius <= BlendConfig.BIOME_BLEND_RADIUS_MAX)
        {
            BlendBuffer blendBuffer = acquireBlendBuffer(blendRadius);

            gatherColors(
                world,
                colorResolver,
                colorType,
                colorCache,
                biomeCache,
                blendBuffer,
                x,
                y,
                z,
                blendRadius);

            int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);

            final int sliceX = x >> sliceSizeLog2;
            final int sliceY = y >> sliceSizeLog2;
            final int sliceZ = z >> sliceSizeLog2;

            final int baseX = sliceX << sliceSizeLog2;
            final int baseY = sliceY << sliceSizeLog2;
            final int baseZ = sliceZ << sliceSizeLog2;

            final int inChunkX = Utility.getLowerBits(baseX, 4);
            final int inChunkY = Utility.getLowerBits(baseY, 4);
            final int inChunkZ = Utility.getLowerBits(baseZ, 4);

            final int base = ColorCaching.getCacheArrayIndex(16, inChunkX, inChunkY, inChunkZ);

            blendColorsForChunk(blendRadius, blendBuffer, result, base);

            releaseBlendBuffer(blendBuffer);
        }
        else
        {
            gatherColorsDirectly(
                world,
                colorResolver,
                x,
                y,
                z,
                result);
        }

        Debug.endEvent(debugEvent);
    }
}
