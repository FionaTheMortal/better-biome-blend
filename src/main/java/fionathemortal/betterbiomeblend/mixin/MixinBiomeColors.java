package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import fionathemortal.betterbiomeblend.BlendedColorChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.biome.BiomeColors;

@Mixin(BiomeColors.class)
public class MixinBiomeColors 
{
	@Overwrite
	public static int 
	getWaterColor(IBlockDisplayReader worldIn, BlockPos blockPosIn) 
	{
		int x = blockPosIn.getX();
		int z = blockPosIn.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		int dimension = BetterBiomeBlend.currentDimensionID;
		
		BlendedColorChunk chunk = BetterBiomeBlend.getBlendedWaterColorChunk(worldIn, chunkX, chunkZ, dimension);
		
		int blockX = x & 15;
		int blockZ = z & 15;
		
		int blockIndex = blockX | (blockZ << 4);
		int result = chunk.data[blockIndex];
		
		return result;
	}
	
	@Overwrite
	public static int 
	getGrassColor(IBlockDisplayReader worldIn, BlockPos blockPosIn) 
	{
		return 0;
	}

	@Overwrite
	public static int 
	getFoliageColor(IBlockDisplayReader worldIn, BlockPos blockPosIn) 
	{
		return 0;
	}
}
