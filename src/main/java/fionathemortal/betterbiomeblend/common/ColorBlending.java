package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.StreamSupport;

public final class ColorBlending
{
    public static final int SECTION_SIZE_LOG2 = 2;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_LOG2;
    public static final int SECTION_MASK = SECTION_SIZE - 1;
    public static final int SECTION_OFFSET = 2;

    public static final int BLEND_BUFFER_DIM = 11;
    public static final int BLEND_BUFFER_MAX = BLEND_BUFFER_DIM - 1;
    public static final int COLOR_CHUNK_DIM = 4;
    public static final int COLOR_CHUNK_MAX = COLOR_CHUNK_DIM - 1;

    public static final int SAMPLE_SEED_X = 0;
    public static final int SAMPLE_SEED_Y = 0;
    public static final int SAMPLE_SEED_Z = 0;

    public static final ReentrantLock           freeBlendBuffersLock = new ReentrantLock();
    public static final Stack<ColorBlendBuffer> freeBlendBuffers     = new Stack<>();

    public static final byte[]
    neighborParams =
    {
         4, 0,
         4, 4,
         3, 8,
    };

    public static int
    getNeighborRectMin(int index)
    {
        return 0;
    }

    public static int
    getNeighborRectMax(int index)
    {
        int offset = 2 * (index + 1);
        int result = neighborParams[offset + 0];

        return result;
    }

    public static int
    getNeighborRectBlendBufferMin(int index)
    {
        int offset = 2 * (index + 1);
        int result = neighborParams[offset + 1];

        return result;
    }

    public static int
    getCacheArrayIndex(int dim, int x, int y, int z)
    {
        int result = x + z * dim + y * dim * dim;

        return result;
    }

    public static ColorBlendBuffer
    acquireBlendBuffer()
    {
        ColorBlendBuffer result = null;

        freeBlendBuffersLock.lock();

        while (!freeBlendBuffers.empty())
        {
            ColorBlendBuffer buffer = freeBlendBuffers.pop();

            result = buffer;
            break;
        }

        freeBlendBuffersLock.unlock();

        if (result == null)
        {
            result = new ColorBlendBuffer();
        }

        return result;
    }

    public static void
    releaseBlendBuffer(ColorBlendBuffer cache)
    {
        freeBlendBuffersLock.lock();

        freeBlendBuffers.push(cache);

        freeBlendBuffersLock.unlock();
    }

    public static int
    getRandomSamplePosition(int section, int seed)
    {
        int random = Random.noise(section, seed);
        int offset = (random & SECTION_MASK);
        int result = SECTION_OFFSET + (section << SECTION_SIZE_LOG2) + offset;

        return result;
    }

    public static void
    gatherRawColorsForChunk(
        Level         world,
        byte[]        result,
        int           chunkX,
        int           chunkZ,
        ColorResolver colorResolver)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

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
    fillBlendBufferWithDefaultColor(
        int           indexX,
        int           indexY,
        int           indexZ,
        int           defaultColor,
        byte[]        blendBuffer)
    {
        int colorR = Color.RGBAGetR(defaultColor);
        int colorG = Color.RGBAGetG(defaultColor);
        int colorB = Color.RGBAGetB(defaultColor);

        final int cacheMinX = getNeighborRectMin(indexX);
        final int cacheMinY = getNeighborRectMin(indexY);
        final int cacheMinZ = getNeighborRectMin(indexZ);

        final int cacheMaxX = getNeighborRectMax(indexX);
        final int cacheMaxY = getNeighborRectMax(indexY);
        final int cacheMaxZ = getNeighborRectMax(indexZ);

        final int blendMinX = getNeighborRectBlendBufferMin(indexX);
        final int blendMinY = getNeighborRectBlendBufferMin(indexY);
        final int blendMinZ = getNeighborRectBlendBufferMin(indexZ);

        final int cacheSizeX = cacheMaxX - cacheMinX;
        final int cacheSizeY = cacheMaxY - cacheMinY;
        final int cacheSizeZ = cacheMaxZ - cacheMinZ;

        final int blendDim = BLEND_BUFFER_DIM;

        for (int y = 0;
             y < cacheSizeY;
             ++y)
        {
            for (int z = 0;
                 z < cacheSizeZ;
                 ++z)
            {
                for (int x = 0;
                     x < cacheSizeX;
                     ++x)
                {
                    int blendX = x + blendMinX;
                    int blendY = y + blendMinY;
                    int blendZ = z + blendMinZ;

                    int blendIndex = getCacheArrayIndex(blendDim, blendX, blendY, blendZ);

                    blendBuffer[3 * blendIndex + 0] = (byte)colorR;
                    blendBuffer[3 * blendIndex + 1] = (byte)colorG;
                    blendBuffer[3 * blendIndex + 2] = (byte)colorB;
                }
            }
        }
    }

    public static void
    gatherColors(
        Level         world,
        ColorResolver colorResolver,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        int           indexX,
        int           indexY,
        int           indexZ,
        byte[]        cachedColors,
        Biome[]       cachedBiomes,
        byte[]        blendBuffer,
        boolean       genNewColors,
        int           defaultColor)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        final int cacheMinX = getNeighborRectMin(indexX);
        final int cacheMinY = getNeighborRectMin(indexY);
        final int cacheMinZ = getNeighborRectMin(indexZ);

        final int cacheMaxX = getNeighborRectMax(indexX);
        final int cacheMaxY = getNeighborRectMax(indexY);
        final int cacheMaxZ = getNeighborRectMax(indexZ);

        final int blendMinX = getNeighborRectBlendBufferMin(indexX);
        final int blendMinY = getNeighborRectBlendBufferMin(indexY);
        final int blendMinZ = getNeighborRectBlendBufferMin(indexZ);

        final int cacheSizeX = cacheMaxX - cacheMinX;
        final int cacheSizeY = cacheMaxY - cacheMinY;
        final int cacheSizeZ = cacheMaxZ - cacheMinZ;

        final int cacheDim = COLOR_CHUNK_DIM;
        final int blendDim = BLEND_BUFFER_DIM;

        final int sectionX = SECTION_SIZE * chunkX;
        final int sectionY = SECTION_SIZE * chunkY;
        final int sectionZ = SECTION_SIZE * chunkZ;

        for (int y = 0;
            y < cacheSizeY;
            ++y)
        {
            for (int z = 0;
                 z < cacheSizeZ;
                 ++z)
            {
                for (int x = 0;
                     x < cacheSizeX;
                     ++x)
                {
                    int cacheX = x + cacheMinX;
                    int cacheY = y + cacheMinY;
                    int cacheZ = z + cacheMinZ;

                    int blendX = x + blendMinX;
                    int blendY = y + blendMinY;
                    int blendZ = z + blendMinZ;

                    int cacheIndex = getCacheArrayIndex(cacheDim, cacheX, cacheY, cacheZ);
                    int blendIndex = getCacheArrayIndex(blendDim, blendX, blendY, blendZ);

                    int cachedR = 0xFF & cachedColors[3 * cacheIndex + 0];
                    int cachedG = 0xFF & cachedColors[3 * cacheIndex + 1];
                    int cachedB = 0xFF & cachedColors[3 * cacheIndex + 2];

                    /* NOTE:
                     * Treat white as uninitialized data. This is an assumption the vanilla code makes.
                     * Not that most people making custom biomes would probably know that.
                     */

                    int commonBits = cachedR & cachedG & cachedB;

                    if (commonBits == 0xFF)
                    {
                        if (genNewColors)
                        {
                            Biome biome = cachedBiomes[cacheIndex];

                            int sampleX = getRandomSamplePosition(sectionX, SAMPLE_SEED_X);
                            int sampleY = getRandomSamplePosition(sectionY, SAMPLE_SEED_Y);
                            int sampleZ = getRandomSamplePosition(sectionZ, SAMPLE_SEED_Z);

                            if (biome == null)
                            {
                                blockPos.set(sampleX, sampleY, sampleZ);

                                biome = world.getBiome(blockPos);

                                cachedBiomes[cacheIndex] = biome;
                            }

                            double blockXF64 = (double)sampleX;
                            double blockZF64 = (double)sampleZ;

                            int color = colorResolver.getColor(biome, blockXF64, blockZF64);

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

                    blendBuffer[3 * blendIndex + 0] = (byte)cachedR;
                    blendBuffer[3 * blendIndex + 1] = (byte)cachedG;
                    blendBuffer[3 * blendIndex + 2] = (byte)cachedB;
                }
            }
        }
    }

    public static int
    gatherColorsForCenterChunk(
        Level         world,
        ColorResolver colorResolver,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        byte[]        cachedColors,
        Biome[]       cachedBiomes,
        byte[]        blendBuffer,
        boolean       skipBoundary)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        final int cacheMinX = getNeighborRectMin(0);
        final int cacheMinY = getNeighborRectMin(0);
        final int cacheMinZ = getNeighborRectMin(0);

        int cacheMaxX = getNeighborRectMax(0);
        int cacheMaxY = getNeighborRectMax(0);
        int cacheMaxZ = getNeighborRectMax(0);

        final int blendMinX = getNeighborRectBlendBufferMin(0);
        final int blendMinY = getNeighborRectBlendBufferMin(0);
        final int blendMinZ = getNeighborRectBlendBufferMin(0);

        if (skipBoundary)
        {
            cacheMaxX -= 1;
            cacheMaxZ -= 1;
        }

        int cacheSizeX = cacheMaxX - cacheMinX;
        int cacheSizeY = cacheMaxY - cacheMinY;
        int cacheSizeZ = cacheMaxZ - cacheMinZ;

        final int cacheDim = COLOR_CHUNK_DIM;
        final int blendDim = BLEND_BUFFER_DIM;

        final int sectionX = SECTION_SIZE * chunkX;
        final int sectionY = SECTION_SIZE * chunkY;
        final int sectionZ = SECTION_SIZE * chunkZ;

        float accumulatedR = 0;
        float accumulatedG = 0;
        float accumulatedB = 0;

        // TODO: Think about the order of loops.

        for (int y = 0;
             y < cacheSizeY;
             ++y)
        {
            for (int z = 0;
                 z < cacheSizeZ;
                 ++z)
            {
                for (int x = 0;
                     x < cacheSizeX;
                     ++x)
                {
                    int cacheX = x + cacheMinX;
                    int cacheY = y + cacheMinY;
                    int cacheZ = z + cacheMinZ;

                    int blendX = x + blendMinX;
                    int blendY = y + blendMinY;
                    int blendZ = z + blendMinZ;

                    int cacheIndex = getCacheArrayIndex(cacheDim, cacheX, cacheY, cacheZ);
                    int blendIndex = getCacheArrayIndex(blendDim, blendX, blendY, blendZ);

                    int cachedR = 0xFF & cachedColors[3 * cacheIndex + 0];
                    int cachedG = 0xFF & cachedColors[3 * cacheIndex + 1];
                    int cachedB = 0xFF & cachedColors[3 * cacheIndex + 2];

                    /* NOTE:
                     * Treat white as uninitialized data. This is an assumption the vanilla code makes.
                     * Not that most people making custom biomes would probably know that.
                     */

                    int commonBits = cachedR & cachedG & cachedB;

                    if (commonBits == 0xFF)
                    {
                        Biome biome = cachedBiomes[cacheIndex];

                        int sampleX = getRandomSamplePosition(sectionX, SAMPLE_SEED_X);
                        int sampleY = getRandomSamplePosition(sectionY, SAMPLE_SEED_Y);
                        int sampleZ = getRandomSamplePosition(sectionZ, SAMPLE_SEED_Z);

                        if (biome == null)
                        {
                            blockPos.set(sampleX, sampleY, sampleZ);

                            biome = world.getBiome(blockPos);

                            cachedBiomes[cacheIndex] = biome;
                        }

                        double blockXF64 = (double)sampleX;
                        double blockZF64 = (double)sampleZ;

                        int color = colorResolver.getColor(biome, blockXF64, blockZF64);

                        cachedR = Color.RGBAGetR(color);
                        cachedG = Color.RGBAGetG(color);
                        cachedB = Color.RGBAGetB(color);

                        cachedColors[3 * cacheIndex + 0] = (byte)cachedR;
                        cachedColors[3 * cacheIndex + 1] = (byte)cachedG;
                        cachedColors[3 * cacheIndex + 2] = (byte)cachedB;
                    }

                    accumulatedR += Color.sRGBByteToLinearFloat(cachedR);
                    accumulatedG += Color.sRGBByteToLinearFloat(cachedG);
                    accumulatedB += Color.sRGBByteToLinearFloat(cachedB);

                    blendBuffer[3 * blendIndex + 0] = (byte)cachedR;
                    blendBuffer[3 * blendIndex + 1] = (byte)cachedG;
                    blendBuffer[3 * blendIndex + 2] = (byte)cachedB;
                }
            }
        }

        // TODO: We forgot to linearize before averaging. Fix this bug in previous versions.

        float averageR = accumulatedR / (cacheSizeX * cacheSizeY * cacheSizeZ);
        float averageG = accumulatedG / (cacheSizeX * cacheSizeY * cacheSizeZ);
        float averageB = accumulatedB / (cacheSizeX * cacheSizeY * cacheSizeZ);

        int sRGBAverageR = Color.linearFloatTosRGBByte(averageR);
        int sRGBAverageG = Color.linearFloatTosRGBByte(averageG);
        int sRGBAverageB = Color.linearFloatTosRGBByte(averageB);

        int result = Color.makeRGBAWithFullAlpha(sRGBAverageR, sRGBAverageG, sRGBAverageB);

        return result;
    }

    public static void
    fillCenterChunkBoundaryWithDefaultColor(byte[] blendBuffer, int defaultColor)
    {
        final int blendMinX = getNeighborRectBlendBufferMin(0);
        final int blendMinY = getNeighborRectBlendBufferMin(0);
        final int blendMinZ = getNeighborRectBlendBufferMin(0);

        final int defaultR = Color.RGBAGetR(defaultColor);
        final int defaultG = Color.RGBAGetG(defaultColor);
        final int defaultB = Color.RGBAGetB(defaultColor);

        // TODO: This loop wasn't touching all blocks of the boundary. Fix this in previous versions.

        for (int y = 0;
             y < COLOR_CHUNK_DIM;
             ++y)
        {
            for (int z = 0;
                 z < COLOR_CHUNK_DIM;
                 ++z)
            {
                for (int x = 0;
                     x < COLOR_CHUNK_DIM;
                     ++x)
                {
                    if (x == COLOR_CHUNK_MAX ||
                        z == COLOR_CHUNK_MAX)
                    {
                        int blendX = x + blendMinX;
                        int blendY = y + blendMinY;
                        int blendZ = z + blendMinZ;

                        int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, blendX, blendY, blendZ);

                        blendBuffer[blendIndex + 0] = (byte)defaultR;
                        blendBuffer[blendIndex + 1] = (byte)defaultG;
                        blendBuffer[blendIndex + 2] = (byte)defaultB;
                    }
                }
            }
        }
    }

    public static void
    gatherRawColorsToCaches(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        byte[]        blendBuffer)
    {
        boolean       neighborsAreLoaded = true;
        ChunkAccess[] neighbors          = new ChunkAccess[9];

        int neighborIndex = 0;

        for (int x = -1;
            x <= 1;
            ++x)
        {
            for (int z = -1;
                z <= 1;
                ++z)
            {
                int neighborX = chunkX + x;
                int neighborZ = chunkZ + z;

                ChunkAccess chunk = world.getChunk(neighborX, neighborZ, ChunkStatus.BIOMES, false);

                if (chunk != null)
                {
                    neighbors[neighborIndex] = chunk;
                }
                else
                {
                    neighborsAreLoaded = false;
                }

                ++neighborIndex;
            }
        }

        BiomeChunk biomeChunk = biomeCache.getOrDefaultInitializeChunk(chunkX, chunkY, chunkZ);
        ColorChunk colorChunk = colorCache.getOrDefaultInitializeChunk(chunkX, chunkY, chunkZ, colorType);

        // TODO: Check for bugs in default color handling

        final boolean skipBoundary = !neighborsAreLoaded;

        int defaultColor = gatherColorsForCenterChunk(
            world,
            colorResolver,
            chunkX,
            chunkY,
            chunkZ,
            colorChunk.data,
            biomeChunk.data,
            blendBuffer,
            skipBoundary);

        colorCache.releaseChunk(colorChunk);
        biomeCache.releaseChunk(biomeChunk);

        if (skipBoundary)
        {
            fillCenterChunkBoundaryWithDefaultColor(blendBuffer, defaultColor);
        }

        neighborIndex = 0;

        for (int x = -1;
             x <= 1;
             ++x)
        {
            for (int z = -1;
                 z <= 1;
                 ++z)
            {
                for (int y = -1;
                    y <= 1;
                    ++y)
                {
                    if (x != 0 || y != 0 || z != 0)
                    {
                        int neighborX = chunkX + x;
                        int neighborY = chunkY + y;
                        int neighborZ = chunkZ + z;

                        if (neighbors[neighborIndex] != null)
                        {
                            BiomeChunk neighborBiomeChunk = biomeCache.getOrDefaultInitializeChunk(neighborX, neighborY, neighborZ);
                            ColorChunk neighborColorChunk = colorCache.getOrDefaultInitializeChunk(neighborX, neighborY, neighborZ, colorType);

                            final boolean genNewColors = neighborsAreLoaded;

                            gatherColors(
                                world,
                                colorResolver,
                                neighborX,
                                neighborY,
                                neighborZ,
                                x,
                                y,
                                z,
                                neighborColorChunk.data,
                                neighborBiomeChunk.data,
                                blendBuffer,
                                genNewColors,
                                defaultColor);

                            colorCache.releaseChunk(neighborColorChunk);
                            biomeCache.releaseChunk(neighborBiomeChunk);
                        }
                        else
                        {
                            fillBlendBufferWithDefaultColor(
                                x,
                                y,
                                z,
                                defaultColor,
                                blendBuffer);
                        }
                    }
                }

                ++neighborIndex;
            }
        }
    }

    public static void
    blendColorsForChunk(byte[] result, byte[] colors)
    {
        int BLEND_RADIUS = 3;
        int BLEND_DIM = 2 * BLEND_RADIUS + 1;
        int BLEND_COUNT = BLEND_DIM * BLEND_DIM * BLEND_DIM;
        int BLEND_COLOR_CHUNK_DIM = 5;

        for (int y = 0;
             y < BLEND_COLOR_CHUNK_DIM;
             ++y)
        {
            for (int z = 0;
                 z < BLEND_COLOR_CHUNK_DIM;
                 ++z)
            {
                for (int x = 0;
                     x < BLEND_COLOR_CHUNK_DIM;
                     ++x)
                {
                    float accumulatedR = 0;
                    float accumulatedG = 0;
                    float accumulatedB = 0;

                    for (int sourceY = 0;
                        sourceY < BLEND_DIM;
                        ++sourceY)
                    {
                        for (int sourceZ = 0;
                             sourceZ < BLEND_DIM;
                             ++sourceZ)
                        {
                            for (int sourceX = 0;
                                 sourceX < BLEND_DIM;
                                 ++sourceX)
                            {
                                int blendX = x + sourceX;
                                int blendY = y + sourceY;
                                int blendZ = z + sourceZ;

                                int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, blendX, blendY, blendZ);

                                accumulatedR += Color.sRGBByteToLinearFloat(0xFF & colors[3 * blendIndex + 0]);
                                accumulatedG += Color.sRGBByteToLinearFloat(0xFF & colors[3 * blendIndex + 1]);
                                accumulatedB += Color.sRGBByteToLinearFloat(0xFF & colors[3 * blendIndex + 2]);
                            }
                        }
                    }

                    int cacheIndex = getCacheArrayIndex(BLEND_COLOR_CHUNK_DIM, x, y, z);

                    float colorR = accumulatedR / BLEND_COUNT;
                    float colorG = accumulatedG / BLEND_COUNT;
                    float colorB = accumulatedB / BLEND_COUNT;

                    byte byteR = Color.linearFloatTosRGBByte(colorR);
                    byte byteG = Color.linearFloatTosRGBByte(colorG);
                    byte byteB = Color.linearFloatTosRGBByte(colorB);

                    result[3 * cacheIndex + 0] = byteR;
                    result[3 * cacheIndex + 1] = byteG;
                    result[3 * cacheIndex + 2] = byteB;
                }
            }
        }
    }

    public  static final AtomicLong    benchmarkStart   = new AtomicLong();
    private static final AtomicLong    totalNanoseconds = new AtomicLong();
    private static final AtomicInteger totalCallCount   = new AtomicInteger();

    private static final AtomicLong    intervalNanoseconds = new AtomicLong();
    private static final AtomicLong    intervalStart       = new AtomicLong();
    private static final AtomicInteger intervalCallCount   = new AtomicInteger();

    public static void
    generateBlendedColorChunk(
        Level         world,
        ColorResolver colorResolverIn,
        int           colorType,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        ColorCache    colorCache,
        BiomeCache    biomeCache,
        byte[]        result)
    {
        ColorBlendBuffer blendBuffer = acquireBlendBuffer();

        boolean measureOverhead = BetterBiomeBlendClient.measureOverhead;
        long    startClock = 0;

        if (measureOverhead)
        {
            startClock = System.nanoTime();

            if (benchmarkStart.get() == 0)
            {
                intervalStart.set(startClock);
                benchmarkStart.set(startClock);
            }
        }

        gatherRawColorsToCaches(
            world,
            colorResolverIn,
            colorType,
            chunkX,
            chunkY,
            chunkZ,
            colorCache,
            biomeCache,
            blendBuffer.color);

        blendColorsForChunk(result, blendBuffer.color);

        if (measureOverhead)
        {
            long stopClock   = System.nanoTime();
            long elapsedTime = stopClock - startClock;

            long totalElapsedTime = intervalNanoseconds.addAndGet(elapsedTime);
            int  totalCallCount   = intervalCallCount.incrementAndGet();

            if (totalCallCount == 100)
            {
                intervalCallCount.addAndGet(-100);

                long intervalStartClock            = intervalStart.get();
                long elapsedTimeSinceIntervalStart = stopClock - intervalStartClock;

                intervalStart.set(stopClock);
                intervalNanoseconds.addAndGet(-totalElapsedTime);
                intervalCallCount.addAndGet(-totalCallCount);

                long elapsedTimeSinceIntervalStartInMicroseconds = elapsedTimeSinceIntervalStart / 1000;
                long totalElapsedTimeInMicroseconds              = totalElapsedTime / 1000;

                String infoString = String.format(
                    "chunks, wall time, calls per second, cpu time, avrg cpu time: %d, %d, %f, %d, %d",
                    totalCallCount,
                    elapsedTimeSinceIntervalStartInMicroseconds,
                    ((double)totalCallCount / (double)elapsedTimeSinceIntervalStartInMicroseconds * 1000000),
                    totalElapsedTimeInMicroseconds,
                    totalElapsedTimeInMicroseconds / totalCallCount);

                BetterBiomeBlendClient.LOGGER.info(infoString);
            }
        }

        releaseBlendBuffer(blendBuffer);
    }
}
