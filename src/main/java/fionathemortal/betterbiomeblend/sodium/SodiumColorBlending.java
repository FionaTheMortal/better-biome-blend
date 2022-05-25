package fionathemortal.betterbiomeblend.sodium;

import fionathemortal.betterbiomeblend.common.Color;
import fionathemortal.betterbiomeblend.common.ColorBlendBuffer;
import fionathemortal.betterbiomeblend.common.Random;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.BiomeManager;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class SodiumColorBlending
{
    public static final int SECTION_SIZE_LOG2 = 2;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_LOG2;
    public static final int SECTION_MASK = SECTION_SIZE - 1;
    public static final int SECTION_OFFSET = 2;
    public static final int BLEND_BUFFER_DIM = 11;

    public static final int SAMPLE_SEED_X = 1664525;
    public static final int SAMPLE_SEED_Y = 214013;
    public static final int SAMPLE_SEED_Z = 16807;

    public static final ReentrantLock           freeBlendBuffersLock = new ReentrantLock();
    public static final Stack<ColorBlendBuffer> freeBlendBuffers     = new Stack<>();

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

    public static Biome
    getBiomeAtPosition(BiomeManager world, BlockPos blockPosition)
    {
        Holder<Biome> biomeHolder = world.getBiome(blockPosition);

        Biome result = null;

        if (biomeHolder.isBound())
        {
            result = biomeHolder.value();
        }

        return result;
    }

    public static void
    gatherRawColorsToCaches(
        BiomeManager  biomeManager,
        ColorResolver colorResolver,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        float[]       blendBuffer)
    {
        final int sectionBaseX = SECTION_SIZE * (chunkX - 1);
        final int sectionBaseY = SECTION_SIZE * (chunkY - 1);
        final int sectionBaseZ = SECTION_SIZE * (chunkZ - 1);

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for (int y = 0;
             y < BLEND_BUFFER_DIM;
             ++y)
        {
            for (int z = 0;
                 z < BLEND_BUFFER_DIM;
                 ++z)
            {
                for (int x = 0;
                     x < BLEND_BUFFER_DIM;
                     ++x)
                {
                    int blendIndex = getCacheArrayIndex(BLEND_BUFFER_DIM, x, y, z);

                    int sectionX = sectionBaseX + x;
                    int sectionY = sectionBaseY + y;
                    int sectionZ = sectionBaseZ + z;

                    int sampleX = getRandomSamplePosition(sectionX, SAMPLE_SEED_X);
                    int sampleY = getRandomSamplePosition(sectionY, SAMPLE_SEED_Y);
                    int sampleZ = getRandomSamplePosition(sectionZ, SAMPLE_SEED_Z);

                    blockPos.set(sampleX, sampleY, sampleZ);

                    int color = 0;

                    Biome biome = getBiomeAtPosition(biomeManager, blockPos);

                    if (biome != null)
                    {
                        double blockXF64 = (double)sampleX;
                        double blockZF64 = (double)sampleZ;

                        color = colorResolver.getColor(biome, blockXF64, blockZF64);
                    }

                    int cachedR = Color.RGBAGetR(color);
                    int cachedG = Color.RGBAGetG(color);
                    int cachedB = Color.RGBAGetB(color);

                    blendBuffer[3 * blendIndex + 0] = Color.sRGBByteToLinearFloat((byte)cachedR);
                    blendBuffer[3 * blendIndex + 1] = Color.sRGBByteToLinearFloat((byte)cachedG);
                    blendBuffer[3 * blendIndex + 2] = Color.sRGBByteToLinearFloat((byte)cachedB);
                }
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
        BiomeManager  biomeManager,
        ColorResolver colorResolverIn,
        int           chunkX,
        int           chunkY,
        int           chunkZ,
        byte[]        result)
    {
        ColorBlendBuffer blendBuffer = acquireBlendBuffer();

        boolean    debugEnabled = Debug.measurePerformance;
        DebugEvent debugEvent   = null;

        if (debugEnabled)
        {
            debugEvent = Debug.pushGenBegin(chunkX, chunkY, chunkZ, 0);
        }

        gatherRawColorsToCaches(
                biomeManager,
                colorResolverIn,
                chunkX,
                chunkY,
                chunkZ,
                blendBuffer.color);

        blendColorsForChunk(result, blendBuffer.color);

        if (debugEnabled)
        {
            Debug.pushGenEnd(debugEvent);
        }

        releaseBlendBuffer(blendBuffer);
    }
}
