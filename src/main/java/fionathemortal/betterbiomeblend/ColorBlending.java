package fionathemortal.betterbiomeblend;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.level.ColorResolver;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class ColorBlending
{
    public static final ReentrantLock           freeBlendBuffersLock = new ReentrantLock();
    public static final Stack<ColorBlendBuffer> freeBlendBuffers     = new Stack<>();

    public static final byte[]
        neighbourOffsets =
        {
            0,  0,
            -1, -1,
            0, -1,
            1, -1,
            -1,  0,
            1,  0,
            -1,  1,
            0,  1,
            1,  1
        };

    public static final byte[]
        neighbourRectParams =
        {
            0,  0,  0,  0,   0,   0,  0,  0,
            -1, -1,  0,  0, -16, -16,  0,  0,
            0, -1,  0,  0,   0, -16,  0,  0,
            0, -1, -1,  0,  16, -16,  0,  0,
            -1,  0,  0,  0, -16,   0,  0,  0,
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
    getNeighborPosX(int index, int chunkX)
    {
        int offset = getNeighbourOffsetX(index);
        int result = chunkX + offset;

        return result;
    }

    public static int
    getNeighborPosZ(int index, int chunkZ)
    {
        int offset = getNeighbourOffsetZ(index);
        int result = chunkZ + offset;

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

    public static ColorBlendBuffer
    acquireBlendBuffer(int blendRadius)
    {
        ColorBlendBuffer result = null;

        freeBlendBuffersLock.lock();

        while (!freeBlendBuffers.empty())
        {
            ColorBlendBuffer buffer = freeBlendBuffers.pop();

            if (buffer.blendRadius == blendRadius)
            {
                result = buffer;
                break;
            }
        }

        freeBlendBuffersLock.unlock();

        if (result == null)
        {
            result = new ColorBlendBuffer(blendRadius);
        }

        return result;
    }

    public static void
    releaseBlendBuffer(ColorBlendBuffer cache)
    {
        freeBlendBuffersLock.lock();

        int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();

        if (cache.blendRadius == blendRadius)
        {
            freeBlendBuffers.push(cache);
        }

        freeBlendBuffersLock.unlock();
    }

    public static void
    gatherRawColorsForChunk(
        World         world,
        byte[]        result,
        int           chunkX,
        int           chunkZ,
        ColorResolver colorResolver)
    {
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        int blockX = 16 * chunkX;
        int blockZ = 16 * chunkZ;

        int dstIndex = 0;

        double baseXF64 = (double)blockX;
        double baseZF64 = (double)blockZ;

        double zF64 = baseZF64;

        for (int z = 0;
            z < 16;
            ++z)
        {
            double xF64 = baseXF64;

            for (int x = 0;
                x < 16;
                ++x)
            {
                blockPos.set(blockX + x, 0, blockZ + z);

                int color = colorResolver.getColor(world.getBiome(blockPos), xF64, zF64);

                int colorR = Color.RGBAGetR(color);
                int colorG = Color.RGBAGetG(color);
                int colorB = Color.RGBAGetB(color);

                result[3 * dstIndex + 0] = (byte)colorR;
                result[3 * dstIndex + 1] = (byte)colorG;
                result[3 * dstIndex + 2] = (byte)colorB;

                ++dstIndex;

                xF64 += 1.0;
            }

            zF64 += 1.0;
        }
    }

    public static void
    fillBlendCacheWithDefaultColor(
        World         world,
        ColorResolver colorResolver,
        int           blendRadius,
        int           neighborIndex,
        int           defaultColor,
        byte[]        blendBuffer)
    {
        int colorR = Color.RGBAGetR(defaultColor);
        int colorG = Color.RGBAGetG(defaultColor);
        int colorB = Color.RGBAGetB(defaultColor);

        final int cacheMinX = getNeighbourRectMinX(neighborIndex, blendRadius);
        final int cacheMinZ = getNeighbourRectMinZ(neighborIndex, blendRadius);
        final int cacheMaxX = getNeighbourRectMaxX(neighborIndex, blendRadius);
        final int cacheMaxZ = getNeighbourRectMaxZ(neighborIndex, blendRadius);

        final int blendMinX = getNeighbourRectBlendCacheMinX(neighborIndex, blendRadius);
        final int blendMinZ = getNeighbourRectBlendCacheMinZ(neighborIndex, blendRadius);

        final int blendDim = 16 + 2 * blendRadius;

        int blendLine = 3 * (blendMinX + blendMinZ * blendDim);

        for (int z = cacheMinZ;
            z < cacheMaxZ;
            ++z)
        {
            int blendIndex = blendLine;

            for (int x = cacheMinX;
                x < cacheMaxX;
                ++x)
            {
                blendBuffer[blendIndex + 0] = (byte)colorR;
                blendBuffer[blendIndex + 1] = (byte)colorG;
                blendBuffer[blendIndex + 2] = (byte)colorB;

                blendIndex += 3;
            }

            blendLine += 3 * blendDim;

        }
    }

    public static void
    gatherRawColors(
        World         world,
        ColorResolver colorResolver,
        int           chunkX,
        int           chunkZ,
        int           blendRadius,
        int           neighborIndex,
        byte[]        cachedColors,
        Biome[]       cachedBiomes,
        byte[]        blendBuffer,
        boolean       genNewColors,
        int           defaultColor)
    {
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        final int cacheMinX = getNeighbourRectMinX(neighborIndex, blendRadius);
        final int cacheMinZ = getNeighbourRectMinZ(neighborIndex, blendRadius);
        final int cacheMaxX = getNeighbourRectMaxX(neighborIndex, blendRadius);
        final int cacheMaxZ = getNeighbourRectMaxZ(neighborIndex, blendRadius);

        final int blendMinX = getNeighbourRectBlendCacheMinX(neighborIndex, blendRadius);
        final int blendMinZ = getNeighbourRectBlendCacheMinZ(neighborIndex, blendRadius);

        final int cacheDim = 16;
        final int blendDim = 16 + 2 * blendRadius;

        final int blockX = 16 * chunkX;
        final int blockZ = 16 * chunkZ;

        final double baseXF64 = (double)(blockX + cacheMinX);
        final double baseZF64 = (double)(blockZ + cacheMinZ);

        int cacheLine = (cacheMinX + cacheMinZ * cacheDim);
        int blendLine = 3 * (blendMinX + blendMinZ * blendDim);

        double zF64 = baseZF64;

        for (int z = cacheMinZ;
            z < cacheMaxZ;
            ++z)
        {
            int cacheIndex = cacheLine;
            int blendIndex = blendLine;

            double xF64 = baseXF64;

            for (int x = cacheMinX;
                x < cacheMaxX;
                ++x)
            {
                int cachedR = 0xFF & cachedColors[3 * cacheIndex + 0];
                int cachedG = 0xFF & cachedColors[3 * cacheIndex + 1];
                int cachedB = 0xFF & cachedColors[3 * cacheIndex + 2];

                int commonBits = cachedR & cachedG & cachedB;

                if (commonBits == 0xFF)
                {
                    if (genNewColors)
                    {
                        Biome biome = cachedBiomes[cacheIndex];

                        if (biome == null)
                        {
                            blockPos.set(blockX + x, 0, blockZ + z);

                            biome = world.getBiome(blockPos);

                            cachedBiomes[cacheIndex] = biome;
                        }

                        int color = colorResolver.getColor(biome, xF64, zF64);

                        cachedR = Color.RGBAGetR(color);
                        cachedG = Color.RGBAGetG(color);
                        cachedB = Color.RGBAGetB(color);

                        cachedColors[3 * cacheIndex + 0] = (byte)cachedR;
                        cachedColors[3 * cacheIndex + 1] = (byte)cachedG;
                        cachedColors[3 * cacheIndex + 2] = (byte)cachedB;
                    }
                    else
                    {
                        cachedR = Color.RGBAGetR(defaultColor);
                        cachedG = Color.RGBAGetG(defaultColor);
                        cachedB = Color.RGBAGetB(defaultColor);
                    }
                }

                blendBuffer[blendIndex + 0] = (byte)cachedR;
                blendBuffer[blendIndex + 1] = (byte)cachedG;
                blendBuffer[blendIndex + 2] = (byte)cachedB;

                cacheIndex += 1;
                blendIndex += 3;

                xF64 += 1.0;
            }

            blendLine += 3 * blendDim;
            cacheLine += cacheDim;

            zF64 += 1.0;
        }
    }

    public static int
    calculateDefaultColor(int blendRadius, byte[] blendBuffer)
    {
        final int blendMinX = getNeighbourRectBlendCacheMinX(0, blendRadius);
        final int blendMinZ = getNeighbourRectBlendCacheMinZ(0, blendRadius);

        final int smallAreaMinX = blendMinX + 2;
        final int smallAreaMinZ = blendMinZ + 2;

        final int blendDim = 16 + 2 * blendRadius;

        int blendLine = 3 * (smallAreaMinX + smallAreaMinZ * blendDim);

        int accumulatedR = 0;
        int accumulatedG = 0;
        int accumulatedB = 0;

        for (int z = 0;
            z < 12;
            ++z)
        {
            int blendIndex = blendLine;

            for (int x = 0;
                x < 12;
                ++x)
            {
                int colorR = 0xFF & blendBuffer[blendIndex + 0];
                int colorG = 0xFF & blendBuffer[blendIndex + 1];
                int colorB = 0xFF & blendBuffer[blendIndex + 2];

                accumulatedR += colorR;
                accumulatedG += colorG;
                accumulatedB += colorB;

                blendIndex += 3;
            }

            blendLine += 3 * blendDim;
        }

        int averageR = accumulatedR / (12 * 12);
        int averageG = accumulatedG / (12 * 12);
        int averageB = accumulatedB / (12 * 12);

        int result = Color.makeRGBAWithFullAlpha(averageR, averageG, averageB);

        return result;
    }

    public static void
    gatherRawColorsToCaches(
        World         world,
        ColorResolver colorResolver,
        int           colorType,
        int           chunkX,
        int           chunkZ,
        int           blendRadius,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        byte[]        blendBuffer)
    {
        boolean neighborsAreLoaded = true;
        Chunk[] neighbors          = new Chunk[9];

        for (int index = 0;
            index < 9;
            ++index)
        {
            int neighborX = getNeighborPosX(index, chunkX);
            int neighborZ = getNeighborPosZ(index, chunkZ);

            Chunk chunk = world.getChunk(neighborX, neighborZ, ChunkStatus.BIOMES, false);

            if (chunk != null)
            {
                neighbors[index] = chunk;
            }
            else
            {
                neighborsAreLoaded = false;
            }
        }

        int defaultColor = -1;

        for (int index = 0;
            index < 9;
            ++index)
        {
            int neighborX = getNeighborPosX(index, chunkX);
            int neighborZ = getNeighborPosZ(index, chunkZ);

            BiomeChunk biomeChunk = biomeCache.getOrDefaultInitializeChunk(neighborX, neighborZ);
            ColorChunk colorChunk = colorCache.getOrDefaultInitializeChunk(neighborX, neighborZ, colorType);

            if (neighbors[index] != null)
            {
                gatherRawColors(world, colorResolver, neighborX, neighborZ, blendRadius, index, colorChunk.data, biomeChunk.data, blendBuffer, neighborsAreLoaded, defaultColor);
            }
            else
            {
                fillBlendCacheWithDefaultColor(world, colorResolver, blendRadius, index, defaultColor, blendBuffer);
            }

            colorCache.releaseChunk(colorChunk);
            biomeCache.releaseChunk(biomeChunk);

            if (!neighborsAreLoaded && defaultColor == -1)
            {
                defaultColor = calculateDefaultColor(blendRadius, blendBuffer);
            }
        }
    }

    public static void
    blendColorsForChunk(World world, byte[] result, ColorBlendBuffer blendCache)
    {
        float[] R = blendCache.R;
        float[] G = blendCache.G;
        float[] B = blendCache.B;

        int blendRadius = blendCache.blendRadius;
        int blendDim = 2 * blendRadius + 1;
        int blendCacheDim = 16 + 2 * blendRadius;
        int blendCount = blendDim * blendDim;

        for (int x = 0;
            x < blendCacheDim;
            ++x)
        {
            R[x] = Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * x + 0]);
            G[x] = Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * x + 1]);
            B[x] = Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * x + 2]);
        }

        for (int z = 1;
            z < blendDim;
            ++z)
        {
            for (int x = 0;
                x < blendCacheDim;
                ++x)
            {
                R[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 0]);
                G[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 1]);
                B[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 2]);
            }
        }

        for (int z = 0;
            z < 16;
            ++z)
        {
            float accumulatedR = 0;
            float accumulatedG = 0;
            float accumulatedB = 0;

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
                float colorR = accumulatedR / blendCount;
                float colorG = accumulatedG / blendCount;
                float colorB = accumulatedB / blendCount;

                result[3 * (16 * z + x) + 0] = Color.linearFloatTosRGBByte(colorR);
                result[3 * (16 * z + x) + 1] = Color.linearFloatTosRGBByte(colorG);
                result[3 * (16 * z + x) + 2] = Color.linearFloatTosRGBByte(colorB);

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

                    R[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index2 + 0]) - Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index1 + 0]);
                    G[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index2 + 1]) - Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index1 + 1]);
                    B[x] += Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index2 + 2]) - Color.sRGBByteToLinearFloat(0xFF & blendCache.color[index1 + 2]);
                }
            }
        }
    }

    public static void
    generateBlendedColorChunk(
        World         world,
        ColorResolver colorResolverIn,
        int           colorType,
        int           chunkX,
        int           chunkZ,
        ColorCache    rawCache,
        BiomeCache    biomeCache,
        byte[]        result)
    {
        int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();

        if (blendRadius >  BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MIN &&
            blendRadius <= BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MAX)
        {
            ColorBlendBuffer blendBuffer = acquireBlendBuffer(blendRadius);

            gatherRawColorsToCaches(world, colorResolverIn, colorType, chunkX, chunkZ, blendBuffer.blendRadius, rawCache, biomeCache, blendBuffer.color);

            blendColorsForChunk(world, result, blendBuffer);

            releaseBlendBuffer(blendBuffer);
        }
        else
        {
            gatherRawColorsForChunk(world, result, chunkX, chunkZ, colorResolverIn);
        }
    }
}
