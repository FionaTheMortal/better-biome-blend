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
		int result = BetterBiomeBlend.getWaterColor(worldIn, blockPosIn);
		
		return result;
	}
	
	@Overwrite
	public static int 
	getGrassColor(IBlockDisplayReader worldIn, BlockPos blockPosIn) 
	{
		int result = BetterBiomeBlend.getGrassColor(worldIn, blockPosIn);
		
		return result;
	}

	@Overwrite
	public static int 
	getFoliageColor(IBlockDisplayReader worldIn, BlockPos blockPosIn) 
	{
		int result = BetterBiomeBlend.getFoliageColor(worldIn, blockPosIn);
		
		return result;
	}
}
