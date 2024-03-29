package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.BiomeCache;
import fionathemortal.betterbiomeblend.common.cache.BiomeSlice;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.ColorSlice;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

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

    public static final int SAMPLE_SEED_X = 1664525;
    public static final int SAMPLE_SEED_Y = 214013;
    public static final int SAMPLE_SEED_Z = 16807;

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
    fillBlendBufferWithDefaultColor(
        int           indexX,
        int           indexY,
        int           indexZ,
        int           defaultColor,
        float[]       blendBuffer)
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

                    blendBuffer[3 * blendIndex + 0] = Color.sRGBByteToLinearFloat((byte)colorR);
                    blendBuffer[3 * blendIndex + 1] = Color.sRGBByteToLinearFloat((byte)colorG);
                    blendBuffer[3 * blendIndex + 2] = Color.sRGBByteToLinearFloat((byte)colorB);
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
        float[]       blendBuffer,
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

                            int sampleX = getRandomSamplePosition(sectionX + x, SAMPLE_SEED_X);
                            int sampleY = getRandomSamplePosition(sectionY + y, SAMPLE_SEED_Y);
                            int sampleZ = getRandomSamplePosition(sectionZ + z, SAMPLE_SEED_Z);

                            if (biome == null)
                            {
                                blockPos.set(sampleX, sampleY, sampleZ);

                                biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

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

                    blendBuffer[3 * blendIndex + 0] = Color.sRGBByteToLinearFloat((byte)cachedR);
                    blendBuffer[3 * blendIndex + 1] = Color.sRGBByteToLinearFloat((byte)cachedG);
                    blendBuffer[3 * blendIndex + 2] = Color.sRGBByteToLinearFloat((byte)cachedB);
                }
            }
        }
    }

    public static Biome
    getBiomeAtPositionOrDefault(Level world, BlockPos blockPosition)
    {
        Holder<Biome> biomeHolder = world.getBiome(blockPosition);

        Biome result = null;

        if (biomeHolder.isBound())
        {
            result = biomeHolder.value();
        }
        else
        {
            biomeHolder = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(Biomes.PLAINS);

            if (biomeHolder.isBound())
            {
                result = biomeHolder.value();
            }
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

    public static int
    gatherColorsForCenterChunk(
        Level         world,
        ColorResolver colorResolver,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        byte[]        cachedColors,
        Biome[]       cachedBiomes,
        float[]       blendBuffer,
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

                        int sampleX = getRandomSamplePosition(sectionX + x, SAMPLE_SEED_X);
                        int sampleY = getRandomSamplePosition(sectionY + y, SAMPLE_SEED_Y);
                        int sampleZ = getRandomSamplePosition(sectionZ + z, SAMPLE_SEED_Z);

                        if (biome == null)
                        {
                            blockPos.set(sampleX, sampleY, sampleZ);

                            biome = getBiomeAtPositionOrDefaultOrThrow(world, blockPos);

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

                    blendBuffer[3 * blendIndex + 0] = Color.sRGBByteToLinearFloat((byte)cachedR);
                    blendBuffer[3 * blendIndex + 1] = Color.sRGBByteToLinearFloat((byte)cachedG);
                    blendBuffer[3 * blendIndex + 2] = Color.sRGBByteToLinearFloat((byte)cachedB);
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
    fillCenterChunkBoundaryWithDefaultColor(float[] blendBuffer, int defaultColor)
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

                        blendBuffer[3 * blendIndex + 0] = Color.sRGBByteToLinearFloat((byte)defaultR);
                        blendBuffer[3 * blendIndex + 1] = Color.sRGBByteToLinearFloat((byte)defaultG);
                        blendBuffer[3 * blendIndex + 2] = Color.sRGBByteToLinearFloat((byte)defaultB);
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
        float[]       blendBuffer)
    {
        boolean       neighborsAreLoaded = true;
        ChunkAccess[] neighbors          = new ChunkAccess[9];

        int neighborIndex = 0;

        for (int z = -1;
             z <= 1;
             ++z)
        {
            for (int x = -1;
                x <= 1;
                ++x)
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

        BiomeSlice biomeChunk = biomeCache.getOrDefaultInitializeChunk(chunkX, chunkY, chunkZ, 0);
        ColorSlice colorChunk = colorCache.getOrDefaultInitializeChunk(chunkX, chunkY, chunkZ, colorType);

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

        for (int z = -1;
             z <= 1;
             ++z)
        {
            for (int x = -1;
                 x <= 1;
                 ++x)
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
                            BiomeSlice neighborBiomeChunk = biomeCache.getOrDefaultInitializeChunk(neighborX, neighborY, neighborZ, 0);
                            ColorSlice neighborColorChunk = colorCache.getOrDefaultInitializeChunk(neighborX, neighborY, neighborZ, colorType);

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
    blendColorsForChunk(byte[] result, float[] colors)
    {
        int BLEND_RADIUS = 3;
        int BLEND_DIM = 2 * BLEND_RADIUS + 1;
        int BLEND_COUNT = BLEND_DIM * BLEND_DIM * BLEND_DIM;
        int BLEND_COLOR_CHUNK_DIM = 5;

        for (int y = 0;
            y < BLEND_BUFFER_DIM;
            ++y)
        {
            for (int z = 0;
                z < BLEND_BUFFER_DIM;
                ++z)
            {
                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                for (int x = 0;
                    x < BLEND_DIM;
                    ++x)
                {
                    int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);

                    accumulatedR += colors[3 * blendIndex + 0];
                    accumulatedG += colors[3 * blendIndex + 1];
                    accumulatedB += colors[3 * blendIndex + 2];
                }

                for (int x = 0;
                    x < BLEND_COLOR_CHUNK_DIM;
                    ++x)
                {
                    int delIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);
                    int addIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x + BLEND_DIM, y, z);

                    float delR = colors[3 * delIndex + 0];
                    float delG = colors[3 * delIndex + 1];
                    float delB = colors[3 * delIndex + 2];

                    int outIndex = delIndex;

                    colors[3 * outIndex + 0] = accumulatedR;
                    colors[3 * outIndex + 1] = accumulatedG;
                    colors[3 * outIndex + 2] = accumulatedB;

                    if (x < BLEND_COLOR_CHUNK_DIM - 1)
                    {
                        accumulatedR -= delR;
                        accumulatedG -= delG;
                        accumulatedB -= delB;

                        accumulatedR += colors[3 * addIndex + 0];
                        accumulatedG += colors[3 * addIndex + 1];
                        accumulatedB += colors[3 * addIndex + 2];
                    }
                }
            }
        }

        for (int y = 0;
             y < BLEND_BUFFER_DIM;
             ++y)
        {
            for (int x = 0;
                 x < BLEND_COLOR_CHUNK_DIM;
                 ++x)
            {
                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                for (int z = 0;
                     z < BLEND_DIM;
                     ++z)
                {
                    int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);

                    accumulatedR += colors[3 * blendIndex + 0];
                    accumulatedG += colors[3 * blendIndex + 1];
                    accumulatedB += colors[3 * blendIndex + 2];
                }

                for (int z = 0;
                     z < BLEND_COLOR_CHUNK_DIM;
                     ++z)
                {
                    int delIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);
                    int addIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z + BLEND_DIM);

                    float delR = colors[3 * delIndex + 0];
                    float delG = colors[3 * delIndex + 1];
                    float delB = colors[3 * delIndex + 2];

                    int outIndex = delIndex;

                    colors[3 * outIndex + 0] = accumulatedR;
                    colors[3 * outIndex + 1] = accumulatedG;
                    colors[3 * outIndex + 2] = accumulatedB;

                    if (z < BLEND_COLOR_CHUNK_DIM - 1)
                    {
                        accumulatedR -= delR;
                        accumulatedG -= delG;
                        accumulatedB -= delB;

                        accumulatedR += colors[3 * addIndex + 0];
                        accumulatedG += colors[3 * addIndex + 1];
                        accumulatedB += colors[3 * addIndex + 2];
                    }
                }
            }
        }

        for (int z = 0;
             z < BLEND_COLOR_CHUNK_DIM;
             ++z)
        {
            for (int x = 0;
                 x < BLEND_COLOR_CHUNK_DIM;
                 ++x)
            {
                float accumulatedR = 0.0f;
                float accumulatedG = 0.0f;
                float accumulatedB = 0.0f;

                for (int y = 0;
                     y < BLEND_DIM;
                     ++y)
                {
                    int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);

                    accumulatedR += colors[3 * blendIndex + 0];
                    accumulatedG += colors[3 * blendIndex + 1];
                    accumulatedB += colors[3 * blendIndex + 2];
                }

                for (int y = 0;
                     y < BLEND_COLOR_CHUNK_DIM;
                     ++y)
                {
                    int delIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);
                    int addIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y + BLEND_DIM, z);

                    float delR = colors[3 * delIndex + 0];
                    float delG = colors[3 * delIndex + 1];
                    float delB = colors[3 * delIndex + 2];

                    int outIndex = getCacheArrayIndex(BLEND_COLOR_CHUNK_DIM, x, y, z);

                    result[3 * outIndex + 0] = Color.linearFloatTosRGBByte(accumulatedR / BLEND_COUNT);
                    result[3 * outIndex + 1] = Color.linearFloatTosRGBByte(accumulatedG / BLEND_COUNT);
                    result[3 * outIndex + 2] = Color.linearFloatTosRGBByte(accumulatedB / BLEND_COUNT);

                    if (y < BLEND_COLOR_CHUNK_DIM - 1)
                    {
                        accumulatedR -= delR;
                        accumulatedG -= delG;
                        accumulatedB -= delB;

                        accumulatedR += colors[3 * addIndex + 0];
                        accumulatedG += colors[3 * addIndex + 1];
                        accumulatedB += colors[3 * addIndex + 2];
                    }
                }
            }
        }
    }

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

        boolean    debugEnabled = Debug.measurePerformance;
        DebugEvent debugEvent   = null;

        if (debugEnabled)
        {
            debugEvent = Debug.pushGenBegin(chunkX, chunkY, chunkZ, colorType);
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

        if (debugEnabled)
        {
            Debug.pushGenEnd(debugEvent);
        }

        releaseBlendBuffer(blendBuffer);
    }
}
