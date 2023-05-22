package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.ColorSlice;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.concurrent.atomic.AtomicLong;

public final class ColorBlending
{
    public static final ThreadLocal<BlendBuffer> threadLocalBlendBuffer = new ThreadLocal<>();

    public static BlendBuffer
    acquireBlendBuffer(int blendRadius)
    {
        BlendBuffer result = null;
        BlendBuffer buffer = threadLocalBlendBuffer.get();

        if (buffer != null && buffer.blendRadius == blendRadius)
        {
            result = buffer;
        }
        else
        {
            result = new BlendBuffer(blendRadius);
        }

        return result;
    }

    public static void
    releaseBlendBuffer(BlendBuffer buffer)
    {
        threadLocalBlendBuffer.set(buffer);
    }

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
        ColorSlice    colorSlice,
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

        final int dimX = sliceMaxX - sliceMinX;
        final int dimY = sliceMaxY - sliceMinY;
        final int dimZ = sliceMaxZ - sliceMinZ;

        final int blendMinX = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDX);
        final int blendMinY = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDY);
        final int blendMinZ = getBlendMin(blendRadius, blockSizeLog2, sliceSizeLog2, sliceIDZ);

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

        int sliceIndexY = 3 * ColorCaching.getCacheArrayIndex(sliceSize, sliceMinX, sliceMinY, sliceMinZ);
        int blendIndexY = 3 * ColorCaching.getCacheArrayIndex(blendSize, blendMinX, blendMinY, blendMinZ);

        int sliceIndexR = 0;
        int sliceIndexG = 0;
        int sliceIndexB = 0;

        for (int y = 0;
             y < dimY;
             ++y)
        {
            int sliceIndexZ = sliceIndexY;
            int blendIndexZ = blendIndexY;

            for (int z = 0;
                 z < dimZ;
                 ++z)
            {
                int sliceIndex = sliceIndexZ;
                int blendIndex = blendIndexZ;

                for (int x = 0;
                     x < dimX;
                     ++x)
                {
                    // NOTE: White is uninitialized data in vanilla code

                    int cachedR = 0xFF & colorSlice.data[sliceIndex + 0];
                    int cachedG = 0xFF & colorSlice.data[sliceIndex + 1];
                    int cachedB = 0xFF & colorSlice.data[sliceIndex + 2];

                    final int commonBits = cachedR & cachedG & cachedB;

                    final int sampleMinX = worldMinX + (x << blockSizeLog2);
                    final int sampleMinY = worldMinY + (y << blockSizeLog2);
                    final int sampleMinZ = worldMinZ + (z << blockSizeLog2);

                    // TODO: Random sampling

                    final int sampleX = sampleMinX;
                    final int sampleY = sampleMinY;
                    final int sampleZ = sampleMinZ;

                    if (commonBits == 0xFF)
                    {
                        blockPos.set(sampleX, sampleY, sampleZ);

                        Biome biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

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

                    Color.sRGBByteToOKLabs(cachedR, cachedG, cachedB, blendBuffer.colors, blendIndex);

                    sliceIndex += 3;
                    blendIndex += 3;
                }

                sliceIndexZ += 3 * sliceSize;
                blendIndexZ += 3 * blendSize;
            }

            sliceIndexY += 3 * sliceSize * sliceSize;
            blendIndexY += 3 * blendSize * blendSize;
        }
    }

    private static void
    fillBlendBufferWithCenterColor(
        Level         world,
        ColorResolver colorResolver,
        BlendBuffer   blendBuffer,
        int           sliceX,
        int           sliceY,
        int           sliceZ,
        int           sliceSizeLog2)
    {
        int worldCenterX = (sliceX << sliceSizeLog2) + (1 << (sliceSizeLog2 - 1));
        int worldCenterY = (sliceY << sliceSizeLog2) + (1 << (sliceSizeLog2 - 1));
        int worldCenterZ = (sliceZ << sliceSizeLog2) + (1 << (sliceSizeLog2 - 1));

        BlockPos blockPos = new BlockPos(worldCenterX, worldCenterY, worldCenterZ);

        final Biome biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

        final int color = colorResolver.getColor(biome, (float)worldCenterX, (float)worldCenterZ);

        final int colorR = Color.RGBAGetR(color);
        final int colorG = Color.RGBAGetG(color);
        final int colorB = Color.RGBAGetB(color);

        Color.sRGBByteToOKLabs(colorR, colorG, colorB, blendBuffer.colors, 0);

        final float floatR = blendBuffer.colors[0];
        final float floatG = blendBuffer.colors[1];
        final float floatB = blendBuffer.colors[2];

        for (int index = 0;
             index < blendBuffer.colors.length;
             index += 3)
        {
            blendBuffer.colors[index    ] = floatR;
            blendBuffer.colors[index + 1] = floatG;
            blendBuffer.colors[index + 2] = floatB;
        }
    }

    public static void
    gatherColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BlendBuffer   blendBuffer,
        int           x,
        int           y,
        int           z,
        int           blendRadius)
    {
        boolean neighborsAreLoaded = true;

        int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);

        final int sliceX = x >> sliceSizeLog2;
        final int sliceY = y >> sliceSizeLog2;
        final int sliceZ = z >> sliceSizeLog2;

        for (int sliceOffsetZ = -1;
             sliceOffsetZ <= 1;
             ++sliceOffsetZ)
        {
            for (int sliceOffsetX = -1;
                 sliceOffsetX <= 1;
                 ++sliceOffsetX)
            {
                int neighborSliceX = sliceX + sliceOffsetX;
                int neighborSliceZ = sliceZ + sliceOffsetZ;

                int neighborChunkX = neighborSliceX >> (4 - sliceSizeLog2);
                int neighborChunkZ = neighborSliceZ >> (4 - sliceSizeLog2);

                ChunkAccess chunk = world.getChunk(neighborChunkX, neighborChunkZ, ChunkStatus.BIOMES, false);

                if (chunk == null)
                {
                    neighborsAreLoaded = false;
                    break;
                }
            }
        }

        if (neighborsAreLoaded)
        {
            ColorSlice[] colorSlices = new ColorSlice[27];

            colorCache.getOrDefaultInitializeNeighbors(colorSlices, blendRadius, sliceX, sliceY, sliceZ, colorType);

            int sliceIndex = 0;

            for (int sliceOffsetZ = -1;
                 sliceOffsetZ <= 1;
                 ++sliceOffsetZ)
            {
                for (int sliceOffsetX = -1;
                     sliceOffsetX <= 1;
                     ++sliceOffsetX)
                {
                    for (int sliceOffsetY = -1;
                         sliceOffsetY <= 1;
                         ++sliceOffsetY)
                    {
                        final int neighborSliceX = sliceX + sliceOffsetX;
                        final int neighborSliceY = sliceY + sliceOffsetY;
                        final int neighborSliceZ = sliceZ + sliceOffsetZ;

                        ColorSlice colorSlice = colorSlices[sliceIndex];

                        ++sliceIndex;

                        gatherColorsForSlice(
                            world,
                            colorResolver,
                            colorSlice,
                            blendBuffer,
                            blendRadius,
                            sliceOffsetX,
                            sliceOffsetY,
                            sliceOffsetZ,
                            neighborSliceX,
                            neighborSliceY,
                            neighborSliceZ);
                    }
                }
            }

            colorCache.releaseSlices(colorSlices);
        }
        else
        {
            fillBlendBufferWithCenterColor(
                world,
                colorResolver,
                blendBuffer,
                sliceX,
                sliceY,
                sliceZ,
                sliceSizeLog2);
        }
    }

    public static void
    blendColorsForChunk(int blendRadius, BlendBuffer buffer, int[] result, int inputX, int inputY, int inputZ)
    {
        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        final int blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        final int sliceX = inputX >> sliceSizeLog2;
        final int sliceY = inputY >> sliceSizeLog2;
        final int sliceZ = inputZ >> sliceSizeLog2;

        final int baseX = sliceX << sliceSizeLog2;
        final int baseY = sliceY << sliceSizeLog2;
        final int baseZ = sliceZ << sliceSizeLog2;

        final int inChunkX = Utility.getLowerBits(baseX, 4);
        final int inChunkY = Utility.getLowerBits(baseY, 4);
        final int inChunkZ = Utility.getLowerBits(baseZ, 4);

        final int baseIndex = ColorCaching.getCacheArrayIndex(16, inChunkX, inChunkY, inChunkZ);

        final int sliceSize = 1 << sliceSizeLog2;
        final int blockSize = 1 << blockSizeLog2;

        final int blendSize       = BlendConfig.getBlendSize(blendRadius);
        final int blendBufferSize = BlendConfig.getBlendBufferSize(blendRadius);

        final int scaledSliceSize = sliceSize >> blockSizeLog2;
        final int filterSupport   = BlendConfig.getFilterSupport(blendRadius);

        float[] colors   = buffer.colors;
        float[] scanline = buffer.scanline;

        final float factor = (1.0f / blockSize);

        int srcSize = blendSize;
        int dstSize = 0;

        for (int y = 0;
             y < srcSize;
             ++y)
        {
            for (int z = 0;
                 z < srcSize;
                 ++z)
            {
                int srcIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, 0, y, z);
                int dstIndex = 0;

                for (int x = 0;
                     x < srcSize;
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

                dstIndex = 3 * ColorCaching.getCacheArrayIndex(blendBufferSize, 0, y, z);

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

                dstIndex = baseIndex + ColorCaching.getCacheArrayIndex(16, x, 0, z);;

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
        // TODO: Needs to check for loaded neighbors

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
        ColorCache    colorCache,
        int           x,
        int           y,
        int           z,
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
                blendBuffer,
                x,
                y,
                z,
                blendRadius);

            DebugEvent subEvent = Debug.pushSubevent(DebugEventType.SUBEVENT);

            blendColorsForChunk(blendRadius, blendBuffer, result, x, y, z);

            Debug.endEvent(subEvent);

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
