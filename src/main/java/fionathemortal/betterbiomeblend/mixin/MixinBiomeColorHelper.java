package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BiomeColor;
import fionathemortal.betterbiomeblend.BiomeColorType;
import fionathemortal.betterbiomeblend.ColorChunk;
import fionathemortal.betterbiomeblend.ColorChunkCache;
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

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.GRASS, chunkX, chunkZ);

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

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.FOLIAGE, chunkX, chunkZ);

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

            chunk = BiomeColor.getBlendedColorChunk(cache, blockAccess, BiomeColorType.WATER, chunkX, chunkZ);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
