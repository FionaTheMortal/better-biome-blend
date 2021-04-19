package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;
import fionathemortal.betterbiomeblend.BiomeColorType;
import fionathemortal.betterbiomeblend.ColorChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;

@Mixin(value = BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper 
{
	private final static ThreadLocal<ColorChunk> threadLocalWaterChunk   = 
		ThreadLocal.withInitial(
			() -> 
			{ 
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk; 
			});
		
	private final static ThreadLocal<ColorChunk> threadLocalGrassChunk   = 
		ThreadLocal.withInitial(
			() -> 
			{
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk; 
			});
		
	private final static ThreadLocal<ColorChunk> threadLocalFoliageChunk = 
		ThreadLocal.withInitial(
			() -> 
			{ 
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk;
			});
		
	@Overwrite
    public static int 
    getGrassColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
		int x = pos.getX();
		int z = pos.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = null; // BiomeColor.getThreadLocalChunk(threadLocalGrassChunk, chunkX, chunkZ, BiomeColorType.GRASS, blockAccess);

		if (chunk == null)
		{
			chunk = BiomeColor.getBlendedColorChunk(blockAccess, BiomeColorType.GRASS, chunkX, chunkZ);
			
			// BiomeColor.setThreadLocalChunk(threadLocalGrassChunk, chunk, blockAccess);
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

		ColorChunk chunk = null; // BiomeColor.getThreadLocalChunk(threadLocalFoliageChunk, chunkX, chunkZ, BiomeColorType.FOLIAGE, blockAccess);

		if (chunk == null)
		{
			chunk = BiomeColor.getBlendedColorChunk(blockAccess, BiomeColorType.FOLIAGE, chunkX, chunkZ);
			
			// BiomeColor.setThreadLocalChunk(threadLocalFoliageChunk, chunk, blockAccess);
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

		ColorChunk chunk = null; // BiomeColor.getThreadLocalChunk(threadLocalWaterChunk, chunkX, chunkZ, BiomeColorType.WATER, blockAccess);

		if (chunk == null)
		{
			chunk = BiomeColor.getBlendedColorChunk(blockAccess, BiomeColorType.WATER, chunkX, chunkZ);
			
			// BiomeColor.setThreadLocalChunk(threadLocalWaterChunk, chunk, blockAccess);
		}
		
		int result = chunk.getColor(x, z);
		
        return result;        
    }
}
