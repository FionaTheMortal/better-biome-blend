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

import java.util.Arrays;
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
        int           sliceIDX,
        int           sliceIDY,
        int           sliceIDZ,
        int           sliceX,
        int           sliceY,
        int           sliceZ)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        final int blendRadius = blendBuffer.blendRadius;

        final int sliceSizeLog2 = blendBuffer.sliceSizeLog2;
        final int blockSizeLog2 = blendBuffer.blockSizeLog2;

        final int sliceSize = blendBuffer.sliceSize;
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

        if ((blendBuffer.scaledBlendDiameter & 1) != 0)
        {
            worldMinX += blockSizeLog2;
            worldMinY += blockSizeLog2;
            worldMinZ += blockSizeLog2;
        }

        int sliceIndexY = 3 * ColorCaching.getCacheArrayIndex(sliceSize, sliceMinX, sliceMinY, sliceMinZ);
        int blendIndexY =     ColorCaching.getCacheArrayIndex(blendSize, blendMinX, blendMinY, blendMinZ);

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
                    // NOTE: 0xFF is uninitialized data in vanilla code

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

                    Color.sRGBByteToOKLabs(cachedR, cachedG, cachedB, blendBuffer.R, blendBuffer.G, blendBuffer.B, blendIndex);

                    sliceIndex += 3;
                    blendIndex += 1;
                }

                sliceIndexZ += 3 * sliceSize;
                blendIndexZ +=     blendSize;
            }

            sliceIndexY += 3 * sliceSize * sliceSize;
            blendIndexY +=     blendSize * blendSize;
        }
    }

    private static void
    fillBlendBufferWithCenterColor(
        Level         world,
        ColorResolver colorResolver,
        BlendBuffer   blendBuffer,
        int           sliceX,
        int           sliceY,
        int           sliceZ)
    {
        int worldCenterX = (sliceX << blendBuffer.sliceSizeLog2) + (1 << (blendBuffer.sliceSizeLog2 - 1));
        int worldCenterY = (sliceY << blendBuffer.sliceSizeLog2) + (1 << (blendBuffer.sliceSizeLog2 - 1));
        int worldCenterZ = (sliceZ << blendBuffer.sliceSizeLog2) + (1 << (blendBuffer.sliceSizeLog2 - 1));

        BlockPos blockPos = new BlockPos(worldCenterX, worldCenterY, worldCenterZ);

        Biome biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

        int color = colorResolver.getColor(biome, (float)worldCenterX, (float)worldCenterZ);

        final int colorR = Color.RGBAGetR(color);
        final int colorG = Color.RGBAGetG(color);
        final int colorB = Color.RGBAGetB(color);

        Color.sRGBByteToOKLabs(colorR, colorG, colorB, blendBuffer.R, blendBuffer.G, blendBuffer.B, 0);

        final float floatR = blendBuffer.R[0];
        final float floatG = blendBuffer.G[0];
        final float floatB = blendBuffer.B[0];

        for (int index = 0;
             index < blendBuffer.R.length;
             ++index)
        {
            blendBuffer.R[index] = floatR;
            blendBuffer.G[index] = floatG;
            blendBuffer.B[index] = floatB;
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
        int           z)
    {
        boolean neighborsAreLoaded = true;

        final int sliceX = x >> blendBuffer.sliceSizeLog2;
        final int sliceY = y >> blendBuffer.sliceSizeLog2;
        final int sliceZ = z >> blendBuffer.sliceSizeLog2;

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

                int neighborChunkX = neighborSliceX >> (4 - blendBuffer.sliceSizeLog2);
                int neighborChunkZ = neighborSliceZ >> (4 - blendBuffer.sliceSizeLog2);

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

            colorCache.getOrDefaultInitializeNeighbors(colorSlices, blendBuffer.sliceSize, sliceX, sliceY, sliceZ, colorType);

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
                sliceZ);
        }
    }

    public static void
    blendColorsForChunk(BlendBuffer buffer, int[] result, int inputX, int inputY, int inputZ)
    {
        final int srcSize = BlendConfig.getBlendSize(buffer.blendRadius);
        final int dstSize = BlendConfig.getSliceSize(buffer.blendRadius);

        final int blendBufferDim = BlendConfig.getBlendBufferSize(buffer.blendRadius);

        final int filterSupport = BlendConfig.getFilterSupport(buffer.blendRadius);
        final int scaledDstSize = dstSize >> buffer.blockSizeLog2;

        final int blockSize = buffer.blockSize;

        final float oneOverBlockSize = (1.0f / blockSize);

        final float filter       = (float)(filterSupport - 1) + oneOverBlockSize;
        final float filterScalar = 1.0f / (filter * filter * filter);

        Arrays.fill(buffer.sumR, 0);
        Arrays.fill(buffer.sumG, 0);
        Arrays.fill(buffer.sumB, 0);

        int srcIndexY = 0;
        int dstIndexY = 0;

        for (int y = 0;
            y < srcSize;
            ++y)
        {
            // NOTE: Copy plane to blendBuffer and blend in X direction

            int indexZ = 0;

            for (int z = 0;
                 z < srcSize;
                 ++z)
            {
                int srcIndexX = indexZ + srcIndexY;
                int dstIndexX = indexZ;

                float sumR = 0;
                float sumG = 0;
                float sumB = 0;

                for (int x = 0;
                     x < filterSupport - 1;
                     ++x)
                {
                    sumR += buffer.R[srcIndexX + x];
                    sumG += buffer.G[srcIndexX + x];
                    sumB += buffer.B[srcIndexX + x];
                }

                int lowerOffset = 0;
                int upperOffset = filterSupport - 1;

                for (int x = 0;
                     x < scaledDstSize;
                     ++x)
                {
                    float lowerR = buffer.R[srcIndexX + lowerOffset] * oneOverBlockSize;
                    float lowerG = buffer.G[srcIndexX + lowerOffset] * oneOverBlockSize;
                    float lowerB = buffer.B[srcIndexX + lowerOffset] * oneOverBlockSize;

                    float upperR = buffer.R[srcIndexX + upperOffset] * oneOverBlockSize;
                    float upperG = buffer.G[srcIndexX + upperOffset] * oneOverBlockSize;
                    float upperB = buffer.B[srcIndexX + upperOffset] * oneOverBlockSize;

                    for (int i = 0;
                         i < blockSize;
                         ++i)
                    {
                        sumR += upperR;
                        sumG += upperG;
                        sumB += upperB;

                        buffer.blendR[dstIndexX] = sumR;
                        buffer.blendG[dstIndexX] = sumG;
                        buffer.blendB[dstIndexX] = sumB;

                        sumR -= lowerR;
                        sumG -= lowerG;
                        sumB -= lowerB;

                        ++dstIndexX;
                    }

                    ++srcIndexX;
                }

                indexZ += blendBufferDim;
            }

            if (y < filterSupport - 1)
            {
                int indexX = 0;

                for (int x = 0;
                     x < dstSize;
                     ++x)
                {
                    int srcIndexZ = indexX;
                    int dstIndexZ = indexX + srcIndexY;
                    int sumIndexZ = indexX;

                    float sumR = 0;
                    float sumG = 0;
                    float sumB = 0;

                    for (int z = 0;
                         z < filterSupport - 1;
                         ++z)
                    {
                        sumR += buffer.blendR[srcIndexZ];
                        sumG += buffer.blendG[srcIndexZ];
                        sumB += buffer.blendB[srcIndexZ];

                        srcIndexZ += blendBufferDim;
                    }

                    int lowerOffset = 0;
                    int upperOffset = (filterSupport - 1) * blendBufferDim;

                    srcIndexZ = indexX;

                    for (int z = 0;
                         z < scaledDstSize;
                         ++z)
                    {
                        float lowerR = buffer.blendR[srcIndexZ + lowerOffset] * oneOverBlockSize;
                        float lowerG = buffer.blendG[srcIndexZ + lowerOffset] * oneOverBlockSize;
                        float lowerB = buffer.blendB[srcIndexZ + lowerOffset] * oneOverBlockSize;

                        float upperR = buffer.blendR[srcIndexZ + upperOffset] * oneOverBlockSize;
                        float upperG = buffer.blendG[srcIndexZ + upperOffset] * oneOverBlockSize;
                        float upperB = buffer.blendB[srcIndexZ + upperOffset] * oneOverBlockSize;

                        for (int i = 0;
                             i < blockSize;
                             ++i)
                        {
                            sumR += upperR;
                            sumG += upperG;
                            sumB += upperB;

                            buffer.R[dstIndexZ] = sumR;
                            buffer.G[dstIndexZ] = sumG;
                            buffer.B[dstIndexZ] = sumB;

                            buffer.sumR[sumIndexZ] += sumR;
                            buffer.sumG[sumIndexZ] += sumG;
                            buffer.sumB[sumIndexZ] += sumB;

                            sumR -= lowerR;
                            sumG -= lowerG;
                            sumB -= lowerB;

                            dstIndexZ += blendBufferDim;
                            sumIndexZ += blendBufferDim;
                        }

                        srcIndexZ += blendBufferDim;
                    }

                    ++indexX;
                }
            }
            else
            {
                int indexX = 0;

                for (int x = 0;
                     x < dstSize;
                     ++x)
                {
                    int srcIndexZ = indexX;
                    int dstIndexZ = indexX + srcIndexY;
                    int sumIndexZ = indexX;

                    float sumR = 0;
                    float sumG = 0;
                    float sumB = 0;

                    for (int z = 0;
                         z < filterSupport - 1;
                         ++z)
                    {
                        sumR += buffer.blendR[srcIndexZ];
                        sumG += buffer.blendG[srcIndexZ];
                        sumB += buffer.blendB[srcIndexZ];

                        srcIndexZ += blendBufferDim;
                    }

                    int lowerOffset = 0;
                    int upperOffset = (filterSupport - 1) * blendBufferDim;

                    srcIndexZ = indexX;

                    int finalIndexZ = dstIndexY + indexX;

                    for (int z = 0;
                         z < scaledDstSize;
                         ++z)
                    {
                        float lowerR = buffer.blendR[srcIndexZ + lowerOffset] * oneOverBlockSize;
                        float lowerG = buffer.blendG[srcIndexZ + lowerOffset] * oneOverBlockSize;
                        float lowerB = buffer.blendB[srcIndexZ + lowerOffset] * oneOverBlockSize;

                        float upperR = buffer.blendR[srcIndexZ + upperOffset] * oneOverBlockSize;
                        float upperG = buffer.blendG[srcIndexZ + upperOffset] * oneOverBlockSize;
                        float upperB = buffer.blendB[srcIndexZ + upperOffset] * oneOverBlockSize;

                        int lowerYOffset = -(filterSupport - 1) * blendBufferDim * blendBufferDim;

                        for (int i = 0;
                             i < blockSize;
                             ++i)
                        {
                            sumR += upperR;
                            sumG += upperG;
                            sumB += upperB;

                            buffer.R[dstIndexZ] = sumR;
                            buffer.G[dstIndexZ] = sumG;
                            buffer.B[dstIndexZ] = sumB;

                            float lowerYRV = buffer.R[dstIndexZ + lowerYOffset];
                            float lowerYGV = buffer.G[dstIndexZ + lowerYOffset];
                            float lowerYBV = buffer.B[dstIndexZ + lowerYOffset];

                            float lowerYR = lowerYRV * oneOverBlockSize;
                            float lowerYG = lowerYGV * oneOverBlockSize;
                            float lowerYB = lowerYBV * oneOverBlockSize;

                            float upperYR = sumR * oneOverBlockSize;
                            float upperYG = sumG * oneOverBlockSize;
                            float upperYB = sumB * oneOverBlockSize;

                            float valueR = buffer.sumR[sumIndexZ];
                            float valueG = buffer.sumG[sumIndexZ];
                            float valueB = buffer.sumB[sumIndexZ];

                            for (int j = 0;
                                j < blockSize;
                                ++j)
                            {
                                valueR += upperYR;
                                valueG += upperYG;
                                valueB += upperYB;

                                int finalIndexY = finalIndexZ + 16 * 16 * j;

                                float filterR = valueR * filterScalar;
                                float filterG = valueG * filterScalar;
                                float filterB = valueB * filterScalar;

                                Color.OKLabsTosRGBAInt(filterR, filterG, filterB, result, finalIndexY);

                                valueR -= lowerYR;
                                valueG -= lowerYG;
                                valueB -= lowerYB;
                            }

                            buffer.sumR[sumIndexZ] += sumR - lowerYRV;
                            buffer.sumG[sumIndexZ] += sumG - lowerYGV;
                            buffer.sumB[sumIndexZ] += sumB - lowerYBV;

                            sumR -= lowerR;
                            sumG -= lowerG;
                            sumB -= lowerB;

                            dstIndexZ += blendBufferDim;
                            sumIndexZ += blendBufferDim;

                            finalIndexZ += 16;
                        }

                        srcIndexZ += blendBufferDim;
                    }

                    ++indexX;
                }

                dstIndexY += blockSize * 16 * 16;
            }

            srcIndexY += blendBufferDim * blendBufferDim;
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

                    blockPos.set(worldX, worldY, worldZ);

                    Biome biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

                    final int color = colorResolver.getColor(biome, worldX, worldZ);

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
                z);

            DebugEvent subEvent = Debug.pushSubevent(DebugEventType.SUBEVENT);

            blendColorsForChunk(blendBuffer, result, x, y, z);

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
