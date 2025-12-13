package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.source.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.source.ColorSlice;
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

public final class ColorGather
{
    public static final int SAMPLE_SEED_X = 1664525;
    public static final int SAMPLE_SEED_Y = 214013;
    public static final int SAMPLE_SEED_Z = 16807;

    private static final int UNITIALIZED_COLOR = 0;

    private static final int CHUNK_SAFETY_MARGIN = 2;


    private static boolean
    isRegionLoadedForSampling(
        Level world,
        int   blockMinX,
        int   blockMinZ,
        int   blockMaxX,
        int   blockMaxZ)
    {
        boolean result = true;

        int marginMinX = blockMinX - CHUNK_SAFETY_MARGIN;
        int marginMinZ = blockMinZ - CHUNK_SAFETY_MARGIN;

        int marginMaxX = blockMaxX + CHUNK_SAFETY_MARGIN;
        int marginMaxZ = blockMaxZ + CHUNK_SAFETY_MARGIN;

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
    isBlockLoadedForSampling(
            Level world,
            int   blockMinX,
            int   blockMinZ)
    {
        boolean result = isRegionLoadedForSampling(world, blockMinX, blockMinZ, blockMinX + 1, blockMinZ + 1);

        return result;
    }

    private static boolean
    isRegionLoadedForBlending(Level world, BlendContext blendContext)
    {
        boolean result = isRegionLoadedForSampling(
            world,
            blendContext.sampleBlockMinX,
            blendContext.sampleBlockMinZ,
            blendContext.sampleBlockMaxX,
            blendContext.sampleBlockMaxZ);

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

    public static int
    getRandomSamplePosition(int min, int blockSizeLog2, int seed)
    {
        int blockMask = Utility.lowerBitMask(blockSizeLog2);

        int random = Random.noise(min, seed);
        int offset = random & blockMask;
        int result = min + offset;

        return result;
    }

    private static void
    gatherColorsInSlice(
        Level         world,
        ColorResolver colorResolver,
        BlendContext  blendContext,
        ColorSlice colorSlice,
        int           sliceX,
        int           sliceY,
        int           sliceZ,
        boolean       regionIsLoaded,
        int           defaultColor)
    {
        final BlendConfig blendConfig = blendContext.blendConfig;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int sliceSampleMinX = blendConfig.getSampleFromSlice(sliceX);
        int sliceSampleMinY = blendConfig.getSampleFromSlice(sliceY);
        int sliceSampleMinZ = blendConfig.getSampleFromSlice(sliceZ);

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

                        int sampleBlockX = blendConfig.getBlockFromSample(sampleX);
                        int sampleBlockY = blendConfig.getBlockFromSample(sampleY);
                        int sampleBlockZ = blendConfig.getBlockFromSample(sampleZ);

                        int blockX = getRandomSamplePosition(sampleBlockX, blendConfig.sampleSizeLog2, SAMPLE_SEED_X);
                        int blockY = getRandomSamplePosition(sampleBlockY, blendConfig.sampleSizeLog2, SAMPLE_SEED_Y);
                        int blockZ = getRandomSamplePosition(sampleBlockZ, blendConfig.sampleSizeLog2, SAMPLE_SEED_Z);

                        blockPos.set(blockX, blockY, blockZ);

                        if (regionIsLoaded || isBlockLoadedForSampling(world, blockX, blockZ))
                        {
                            cachedColor =  getColorForBlock(world, blockPos, blockX, blockZ, colorResolver);

                            colorSlice.data[sliceIndex] = cachedColor;
                        }
                        else
                        {
                            cachedColor = defaultColor;
                        }
                    }

                    blendContext.setColorSample(cachedColor, blendIndex);
                }
            }
        }
    }

    private static int
    getCenterColor(
        Level         world,
        ColorResolver colorResolver,
        BlendContext  blendContext)
    {
        int centerX = (blendContext.blockMinX + blendContext.blockMaxX) / 2;
        int centerY = (blendContext.blockMinY + blendContext.blockMaxY) / 2;
        int centerZ = (blendContext.blockMinZ + blendContext.blockMaxZ) / 2;

        BlockPos blockPos = new BlockPos(centerX, centerY, centerZ);

        int result = getColorForBlock(world, blockPos, centerX, centerZ, colorResolver);

        return result;
    }

    public static void
    gatherColorsForBlending(
        Level         world,
        ColorResolver colorResolver,
        int           colorType,
        ColorCache    colorCache,
        BlendContext  blendContext)
    {
        boolean regionIsLoaded = isRegionLoadedForBlending(world, blendContext);
        int     defaultColor   = UNITIALIZED_COLOR;

        if (!regionIsLoaded)
        {
            defaultColor = getCenterColor(world, colorResolver, blendContext);
        }

        final BlendConfig blendConfig = blendContext.blendConfig;

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

                    gatherColorsInSlice(
                        world,
                        colorResolver,
                        blendContext,
                        slice,
                        sliceX,
                        sliceY,
                        sliceZ,
                        regionIsLoaded,
                        defaultColor);

                    colorCache.releaseSlice(slice);
                }
            }
        }

        if (!regionIsLoaded)
        {
            blendContext.colorBitsInclusive = defaultColor;
            blendContext.colorBitsExclusive = defaultColor;
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

        boolean regionIsLoaded = isRegionLoadedForSampling(world, blockMinX, blockMinZ, blockMaxX, blockMaxZ);

        if (regionIsLoaded)
        {

        }
        else
        {
            // TODO: Single color approximation
        }
    }
}
