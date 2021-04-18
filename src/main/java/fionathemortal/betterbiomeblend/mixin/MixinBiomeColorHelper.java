package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;

@Mixin(value = BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper 
{
	@Overwrite
    public static int 
    getGrassColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
        return 0;
    }

	@Overwrite
    public static int 
    getFoliageColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
    	 return 0;
    }

	@Overwrite
    public static int 
    getWaterColorAtPos(IBlockAccess blockAccess, BlockPos pos)
    {
    	 return 0;
    }
}
