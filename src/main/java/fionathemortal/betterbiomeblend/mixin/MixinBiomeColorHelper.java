package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper
{
    @Overwrite
    public static int
    getColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        int x = pos.getX();
        int z = pos.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        int colorResolverID = ColorResolverCompatibility.getColorResolverID(colorResolver);

        ThreadLocal<ColorChunk> threadLocal = BiomeColor.getThreadLocalGenericChunkWrapper(blockAccess);

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocal, chunkX, chunkZ, colorResolverID);

        if (chunk == null)
        {
            ColorChunkCache cache = BiomeColor.getColorChunkCacheForIBlockAccess(blockAccess);

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, colorResolverID, chunkX, chunkZ, colorResolver);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }

    @Overwrite
    public static int
    getGrassColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
        int x = pos.getX();
        int z = pos.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        ThreadLocal<ColorChunk> threadLocal = BiomeColor.getThreadLocalGrassChunkWrapper(blockAccess);

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocal, chunkX, chunkZ, BiomeColorType.GRASS);

        if (chunk == null)
        {
            ColorChunkCache cache = BiomeColor.getColorChunkCacheForIBlockAccess(blockAccess);

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.GRASS, chunkX, chunkZ, BiomeColorHelper.GRASS_COLOR);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }

    @Overwrite
    public static int
    getFoliageColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
        int x = pos.getX();
        int z = pos.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        ThreadLocal<ColorChunk> threadLocal = BiomeColor.getThreadLocalFoliageChunkWrapper(blockAccess);

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocal, chunkX, chunkZ, BiomeColorType.FOLIAGE);

        if (chunk == null)
        {
            ColorChunkCache cache = BiomeColor.getColorChunkCacheForIBlockAccess(blockAccess);

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.FOLIAGE, chunkX, chunkZ, BiomeColorHelper.FOLIAGE_COLOR);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }

    @Overwrite
    public static int
    getWaterColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
        int x = pos.getX();
        int z = pos.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        ThreadLocal<ColorChunk> threadLocal = BiomeColor.getThreadLocalWaterChunkWrapper(blockAccess);

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocal, chunkX, chunkZ, BiomeColorType.WATER);

        if (chunk == null)
        {
            ColorChunkCache cache = BiomeColor.getColorChunkCacheForIBlockAccess(blockAccess);

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.WATER, chunkX, chunkZ, BiomeColorHelper.WATER_COLOR);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
