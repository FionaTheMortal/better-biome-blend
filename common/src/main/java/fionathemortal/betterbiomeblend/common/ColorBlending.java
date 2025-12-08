package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.ColorSlice;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public final class ColorBlending
{
    public static final int SAMPLE_SEED_X = 1664525;
    public static final int SAMPLE_SEED_Y = 214013;
    public static final int SAMPLE_SEED_Z = 16807;

    private static final int UNITIALIZED_COLOR = 0;

    private static final ThreadLocal<BlendContext> threadLocalBlendContext = new ThreadLocal<>();

    private static BlendContext
    acquireBlendContext()
    {
        BlendConfig config = BetterBiomeBlendClient.getCurrentBlendConfig();

        BlendContext result = threadLocalBlendContext.get();

        if (result == null || result.blendConfig != config)
        {
            result = new BlendContext(config);
        }

        return result;
    }

    private static void
    releaseBlendContext(BlendContext context)
    {
        context.free();

        threadLocalBlendContext.set(context);
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
    getBiomeForBlockOrDefault(Level world, BlockPos blockPosition)
    {
        Biome result;

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

    public static int
    getColorForBlock(Level world, BlockPos blockPos, float posX, float posZ, ColorResolver colorResolver)
    {
        int result;

        Biome biome = getBiomeForBlockOrDefault(world, blockPos);

        if (biome != null)
        {
            result = colorResolver.getColor(biome, posX, posZ);
        }
        else
        {
            result = Color.DEBUG_PINK;
        }

        return result;
    }

    private static int
    getColorForSample(
        Level                    world,
        ColorResolver            colorResolver,
        BlendConfig              blendConfig,
        BlockPos.MutableBlockPos blockPos,
        int                      sampleX,
        int                      sampleY,
        int                      sampleZ)
    {
        int sampleBlockX = blendConfig.getBlockFromSample(sampleX);
        int sampleBlockY = blendConfig.getBlockFromSample(sampleY);
        int sampleBlockZ = blendConfig.getBlockFromSample(sampleZ);

        int blockX = getRandomSamplePosition(sampleBlockX, blendConfig.sampleSizeLog2, SAMPLE_SEED_X);
        int blockY = getRandomSamplePosition(sampleBlockY, blendConfig.sampleSizeLog2, SAMPLE_SEED_Y);
        int blockZ = getRandomSamplePosition(sampleBlockZ, blendConfig.sampleSizeLog2, SAMPLE_SEED_Z);

        blockPos.set(blockX, blockY, blockZ);

        int result = getColorForBlock(world, blockPos, blockX, blockZ, colorResolver);

        return result;
    }

    public static int
    getRandomSamplePosition(int min, int blockSizeLog2, int seed)
    {
        int blockMask = Utility.lowerBitMask(blockSizeLog2);

        int random = Random.noise(min, seed);
        int offset = random & blockMask;
        int result = min + offset;

        return result;
    }

    private static boolean
    isRegionLoaded(
        Level world,
        int   blockMinX,
        int   blockMinZ,
        int   blockMaxX,
        int   blockMaxZ)
    {
        boolean result = true;

        int marginMinX = blockMinX - 2;
        int marginMinZ = blockMinZ - 2;

        int marginMaxX = blockMaxX + 2;
        int marginMaxZ = blockMaxZ + 2;

        int chunkMinX = Utility.blockToChunk(marginMinX);
        int chunkMinZ = Utility.blockToChunk(marginMinZ);

        int chunkMaxX = Utility.blockToChunk(marginMaxX + 15);
        int chunkMaxZ = Utility.blockToChunk(marginMaxZ + 15);

    outerLoop:
        for (int chunkZ = chunkMinZ;
             chunkZ < chunkMaxZ;
             ++chunkZ)
        {
            for (int chunkX = chunkMinX;
                 chunkX < chunkMaxX;
                 ++chunkX)
            {
                ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);

                if (chunk == null)
                {
                    result = false;
                    break outerLoop;
                }
            }
        }

        return result;
    }

    private static boolean
    isRegionLoadedForBlending(Level world, BlendContext blendContext)
    {
        boolean result = isRegionLoaded(
            world,
            blendContext.sampleBlockMinX,
            blendContext.sampleBlockMinZ,
            blendContext.sampleBlockMaxX,
            blendContext.sampleBlockMaxZ);

        return result;
    }

    private static void
    gatherColorsInSliceForBlending(
        Level         world,
        ColorResolver colorResolver,
        BlendContext  blendContext,
        ColorSlice    colorSlice,
        int           sliceX,
        int           sliceY,
        int           sliceZ)
    {
        BlendConfig blendConfig = blendContext.blendConfig;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int sliceSampleMinX = blendConfig.getSampleFromSlice(sliceX);
        int sliceSampleMinY = blendConfig.getSampleFromSlice(sliceX);
        int sliceSampleMinZ = blendConfig.getSampleFromSlice(sliceX);

        int sliceSampleMaxX = blendConfig.getSampleFromSlice(sliceX + 1);
        int sliceSampleMaxY = blendConfig.getSampleFromSlice(sliceY + 1);
        int sliceSampleMaxZ = blendConfig.getSampleFromSlice(sliceZ + 1);

        int sampleMinX = Math.max(blendContext.sampleMinX, sliceSampleMinX);
        int sampleMinY = Math.max(blendContext.sampleMinY, sliceSampleMinY);
        int sampleMinZ = Math.max(blendContext.sampleMinZ, sliceSampleMinZ);

        int sampleMaxX = Math.min(blendContext.sampleMaxX, sliceSampleMaxX);
        int sampleMaxY = Math.min(blendContext.sampleMaxY, sliceSampleMaxY);
        int sampleMaxZ = Math.min(blendContext.sampleMaxZ, sliceSampleMaxZ);

        int sliceMinX = sampleMinX - sliceSampleMinX;
        int sliceMinY = sampleMinY - sliceSampleMinY;
        int sliceMinZ = sampleMinZ - sliceSampleMinZ;

        int blendMinX = sampleMinX - blendContext.sampleMinX;
        int blendMinY = sampleMinY - blendContext.sampleMinY;
        int blendMinZ = sampleMinZ - blendContext.sampleMinZ;

        int sampleCountX = sampleMaxX - sampleMinX;
        int sampleCountY = sampleMaxY - sampleMinY;
        int sampleCountZ = sampleMaxZ - sampleMinZ;

        for (int z = 0;
             z < sampleCountZ;
             ++z)
        {
            for (int y = 0;
                 y < sampleCountY;
                 ++y)
            {
                for (int x = 0;
                     x < sampleCountX;
                     ++x)
                {
                    int sliceIndex = Array3i.getArrayIndex(
                        blendConfig.sliceSize,
                        blendConfig.sliceSize,
                        sliceMinX + x,
                        sliceMinY + y,
                        sliceMinZ + z);

                    int blendIndex = Array3c.getArrayIndex(
                        blendContext.sampleCountX,
                        blendContext.sampleCountY,
                        blendMinX + x,
                        blendMinY + y,
                        blendMinZ + z);

                    int cachedColor = colorSlice.data[sliceIndex];

                    if (cachedColor == UNITIALIZED_COLOR)
                    {
                        int sampleX = sampleMinX + x;
                        int sampleY = sampleMinY + y;
                        int sampleZ = sampleMinZ + z;

                        cachedColor = getColorForSample(
                            world,
                            colorResolver,
                            blendConfig,
                            blockPos,
                            sampleX,
                            sampleY,
                            sampleZ);

                        colorSlice.data[sliceIndex] = cachedColor;
                    }

                    blendContext.setColorSample(cachedColor, blendIndex);
                }
            }
        }
    }

    private static void
    gatherColorsForBlending(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BlendContext  blendContext)
    {
        BlendConfig blendConfig = blendContext.blendConfig;

        boolean regionIsLoaded = isRegionLoadedForBlending(world, blendContext);

        if (regionIsLoaded)
        {
            // TODO: Test using tryLock again

            for (int sliceZ = blendContext.sliceMinZ;
                sliceZ < blendContext.sliceMaxZ;
                ++sliceZ)
            {
                for (int sliceY = blendContext.sliceMinY;
                     sliceY < blendContext.sliceMaxY;
                     ++sliceY)
                {
                    for (int sliceX = blendContext.sliceMinX;
                         sliceX < blendContext.sliceMaxX;
                         ++sliceX)
                    {
                        ColorSlice slice = colorCache.getOrInitSlice(
                            blendConfig.sliceSize,
                            sliceX,
                            sliceY,
                            sliceZ,
                            colorType,
                            false);

                        gatherColorsInSliceForBlending(
                            world,
                            colorResolver,
                            blendContext,
                            slice,
                            sliceX,
                            sliceY,
                            sliceZ);

                        colorCache.releaseSlice(slice);
                    }
                }
            }
        }
        else
        {
            // TODO: Handle temp output for unitialized data
        }
    }

    private static void
    gatherColorsDirectly(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int[]         output,
        int           outputMinX,
        int           outputMinY,
        int           outputMinZ,
        int           outputDimX,
        int           outputDimY,
        int           outputDimZ,
        int           outputStrideX,
        int           outputStrideY,
        int           outputStrideZ)
    {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int blockMaxX = blockMinX + outputDimX;
        int blockMaxZ = blockMinZ + outputDimZ;

        boolean regionIsLoaded = isRegionLoaded(world, blockMinX, blockMinZ, blockMaxX, blockMaxZ);

        if (regionIsLoaded)
        {

        }
        else
        {
            // TODO: Single color approximation
        }
    }

    private static void
    outputSingularColor(BlendContext blendContext)
    {
        int color = blendContext.colorBitsExclusive;

        blendContext.output.fill(color);
    }

    private static void
    blendColorsInLine(BlendContext blendContext, BlendConfig blendConfig, int lineIndex, int planeIndex)
    {
        int lowerFilter = blendContext.blockMinX - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinX + blendConfig.blendRadius;

        int lineBase = Array3c.getArrayIndex(
            blendContext.sampleCountX,
            blendContext.sampleCountY,
            0,
            lineIndex,
            planeIndex);

        float sumR = 0;
        float sumG = 0;
        float sumB = 0;

        for (int block = lowerFilter;
             block < upperFilter;
             ++block)
        {
            int sampleIndexX = blendConfig.getSampleFromBlock(block) - blendContext.sampleMinX;
            int sampleIndex  = lineBase + sampleIndexX;

            float sampleR = blendContext.samples[sampleIndex    ];
            float sampleG = blendContext.samples[sampleIndex + 1];
            float sampleB = blendContext.samples[sampleIndex + 2];

            sumR += sampleR;
            sumG += sampleG;
            sumB += sampleB;
        }

        int lineRingBufferIndex = (lineIndex % blendContext.lineCount);

        int outputDimX = blendContext.output.dimX;

        int outputStrideX = 3;
        int outputStrideY = 3 * outputDimX;

        int outputIndexY = lineRingBufferIndex * outputStrideY;

        for (int x = 0;
             x < outputDimX;
             ++x)
        {
            int upperSampleIndexX = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinX;
            int upperSampleIndex  = lineBase + upperSampleIndexX;

            float upperSampleR = blendContext.samples[upperSampleIndex    ];
            float upperSampleG = blendContext.samples[upperSampleIndex + 1];
            float upperSampleB = blendContext.samples[upperSampleIndex + 2];

            sumR += upperSampleR;
            sumG += upperSampleG;
            sumB += upperSampleB;

            int outputIndex = outputIndexY + outputStrideX * x;

            blendContext.lineBuffer[outputIndex    ] = sumR;
            blendContext.lineBuffer[outputIndex + 1] = sumG;
            blendContext.lineBuffer[outputIndex + 2] = sumB;

            int lowerSampleIndexX = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinX;
            int lowerSampleIndex  = lineBase + lowerSampleIndexX;

            float lowerSampleR = blendContext.samples[lowerSampleIndex    ];
            float lowerSampleG = blendContext.samples[lowerSampleIndex + 1];
            float lowerSampleB = blendContext.samples[lowerSampleIndex + 2];

            sumR -= lowerSampleR;
            sumG -= lowerSampleG;
            sumB -= lowerSampleB;

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    blendColorsInPlane(BlendContext blendContext, BlendConfig blendConfig, int planeIndex)
    {
        int lowerFilter = blendContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinY + blendConfig.blendRadius;

        int outputDimY = blendContext.output.dimY;
        int outputDimX = blendContext.output.dimX;

        int prevLineIndex = Integer.MIN_VALUE;

        final int outputStrideX = 3;
        final int outputStrideY = 3 * outputDimX;
        final int outputStrideZ = 3 * outputDimX * outputDimY;

        for (int block = lowerFilter;
             block < upperFilter;
             ++block)
        {
            boolean isLastIteration = (block + 1 == upperFilter);

            int lineIndex = blendConfig.getSampleFromBlock(block) - blendContext.sampleMinY;

            if (lineIndex != prevLineIndex || isLastIteration)
            {
                if (lineIndex != prevLineIndex)
                {
                    blendColorsInLine(blendContext, blendConfig, lineIndex, planeIndex);
                }

                int lineBase = (lineIndex % blendContext.lineCount) * outputStrideY;

                for (int x = 0;
                     x < outputDimX;
                     ++x)
                {
                    int sampleIndex = lineBase + x;

                    blendContext.lineSum[x    ] += blendContext.lineBuffer[sampleIndex    ];
                    blendContext.lineSum[x + 1] += blendContext.lineBuffer[sampleIndex + 1];
                    blendContext.lineSum[x + 2] += blendContext.lineBuffer[sampleIndex + 2];
                }
            }
        }

        int planeRingBufferIndex = (planeIndex % blendContext.planeCount);
        int planeRingBufferFirst = planeRingBufferIndex * outputStrideZ;

        for (int y = 0;
             y < outputDimY;
             ++y)
        {
            int upperLineIndex = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinY;
            int lowerLineIndex = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinY;

            if (upperLineIndex != prevLineIndex)
            {
                blendColorsInLine(blendContext, blendConfig, upperLineIndex, planeIndex);

                prevLineIndex = upperLineIndex;
            }

            int upperLineBase = (upperLineIndex % blendContext.lineCount) * outputStrideY;
            int lowerLineBase = (lowerLineIndex % blendContext.lineCount) * outputStrideY;

            int outputIndex = planeRingBufferFirst + y * outputStrideY;

            for (int x = 0;
                 x < outputDimX;
                 ++x)
            {
                int upperSampleIndex = upperLineBase + x;
                int lowerSampleIndex = lowerLineBase + x;

                float colorR = blendContext.lineSum[x    ];
                float colorG = blendContext.lineSum[x + 1];
                float colorB = blendContext.lineSum[x + 2];

                colorR += blendContext.lineBuffer[upperSampleIndex    ];
                colorG += blendContext.lineBuffer[upperSampleIndex + 1];
                colorB += blendContext.lineBuffer[upperSampleIndex + 2];

                blendContext.planeBuffer[outputIndex    ] = colorR;
                blendContext.planeBuffer[outputIndex + 1] = colorG;
                blendContext.planeBuffer[outputIndex + 2] = colorB;

                colorR -= blendContext.lineBuffer[lowerSampleIndex    ];
                colorG -= blendContext.lineBuffer[lowerSampleIndex + 1];
                colorB -= blendContext.lineBuffer[lowerSampleIndex + 2];

                blendContext.lineSum[x    ] = colorR;
                blendContext.lineSum[x + 1] = colorG;
                blendContext.lineSum[x + 2] = colorB;

                outputIndex += outputStrideX;
            }

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    blendColors(BlendContext blendContext)
    {
        // TODO: Filter support math

        BlendConfig blendConfig = blendContext.blendConfig;

        Array3i output = blendContext.output;

        int outputDimZ = output.dimZ;
        int outputDimY = output.dimY;
        int outputDimX = output.dimX;

        int outputIndex = output.first;

        int lowerFilter = blendContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinY + blendConfig.blendRadius;

        int prevPlaneIndex = Integer.MIN_VALUE;

        for (int z = lowerFilter;
             z < upperFilter;
             ++z)
        {
            boolean isLastIteration = (z + 1 == upperFilter);

            int planeIndex = blendConfig.getSampleFromBlock(z) - blendContext.sampleMinZ;

            if (planeIndex != prevPlaneIndex || isLastIteration)
            {
                if (planeIndex != prevPlaneIndex)
                {
                    blendColorsInPlane(blendContext, blendConfig, planeIndex);

                    prevPlaneIndex = planeIndex;
                }

                int planeFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, planeIndex);

                int indexXY = 0;

                for (int y = 0;
                     y < outputDimY;
                     ++y)
                {
                    for (int x = 0;
                         x < outputDimX;
                         ++x)
                    {
                        int sampleIndex = planeFirst + indexXY;

                        blendContext.planeSum[indexXY    ] += blendContext.planeBuffer[sampleIndex    ];
                        blendContext.planeSum[indexXY + 1] += blendContext.planeBuffer[sampleIndex + 1];
                        blendContext.planeSum[indexXY + 2] += blendContext.planeBuffer[sampleIndex + 2];

                        indexXY += 3;
                    }
                }
            }
        }

        for (int z = 0;
             z < outputDimZ;
             ++z)
        {
            int upperPlaneIndex = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinZ;
            int lowerPlaneIndex = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinZ;

            if (upperPlaneIndex != prevPlaneIndex)
            {
                blendColorsInPlane(blendContext, blendConfig, upperPlaneIndex);

                prevPlaneIndex = upperPlaneIndex;
            }

            int upperPlaneRingBufferIndex = (upperPlaneIndex % blendContext.planeCount);
            int lowerPlaneRingBufferIndex = (lowerPlaneIndex % blendContext.planeCount);

            int upperPlaneFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, upperPlaneRingBufferIndex);
            int lowerPlaneFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, lowerPlaneRingBufferIndex);

            int indexXY = 0;

            for (int y = 0;
                 y < outputDimY;
                 ++y)
            {
                for (int x = 0;
                    x < outputDimX;
                    ++x)
                {
                    int upperSampleIndex = upperPlaneFirst + indexXY;
                    int lowerSampleIndex = lowerPlaneFirst + indexXY;

                    float colorR = blendContext.planeSum[indexXY    ];
                    float colorG = blendContext.planeSum[indexXY + 1];
                    float colorB = blendContext.planeSum[indexXY + 2];

                    colorR += blendContext.planeBuffer[upperSampleIndex    ];
                    colorG += blendContext.planeBuffer[upperSampleIndex + 1];
                    colorB += blendContext.planeBuffer[upperSampleIndex + 2];

                    output.data[outputIndex] = Color.OKLabsTosRGBAInt(colorR, colorG, colorB);

                    colorR -= blendContext.planeBuffer[lowerSampleIndex    ];
                    colorG -= blendContext.planeBuffer[lowerSampleIndex + 1];
                    colorB -= blendContext.planeBuffer[lowerSampleIndex + 2];

                    blendContext.planeSum[indexXY    ] = colorR;
                    blendContext.planeSum[indexXY + 1] = colorG;
                    blendContext.planeSum[indexXY + 2] = colorB;

                    outputIndex += output.strideX;

                    indexXY += 3;
                }

                outputIndex += output.strideY;
            }

            ++upperFilter;
            ++lowerFilter;

            outputIndex += output.strideZ;
        }
    }

    public static void
    generateColors(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        int           blockMinX,
        int           blockMinY,
        int           blockMinZ,
        int[]         output,
        int           outputMinX,
        int           outputMinY,
        int           outputMinZ,
        int           outputDimX,
        int           outputDimY,
        int           outputDimZ,
        int           outputDataDimX,
        int           outputDataDimY)
    {
        DebugEvent debugEvent = Debug.pushColorGenEvent(blockMinX, blockMinY, blockMinZ, colorType);

        BlendContext blendContext = acquireBlendContext();

        blendContext.init(
            blockMinX,
            blockMinY,
            blockMinZ,
            output,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputDimX,
            outputDimY,
            outputDimZ,
            outputDataDimX,
            outputDataDimY);

        if (blendContext.blendConfig.blendRadius >  BlendConfig.BIOME_BLEND_RADIUS_MIN &&
            blendContext.blendConfig.blendRadius <= BlendConfig.BIOME_BLEND_RADIUS_MAX)
        {
            gatherColorsForBlending(
                world,
                colorResolver,
                colorType,
                colorCache,
                blendContext);

            if (!blendContext.isSingleColor())
            {
                blendColors(blendContext);
            }
            else
            {
                outputSingularColor(blendContext);
            }
        }
        else
        {
            // gatherColorsDirectly();
        }

        releaseBlendContext(blendContext);

        Debug.endEvent(debugEvent);
    }

    public static void
    generateColorsForBlock(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BlendChunk    blendChunk,
        int           blockX,
        int           blockY,
        int           blockZ)
    {
    }
}
