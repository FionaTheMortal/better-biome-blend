package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockDoublePlant.class)
public abstract class MixinBlockDoublePlant {

    @Shadow
    public abstract int func_149885_e(IBlockAccess worldIn, int x, int y, int z);

    /**
     * @author FionaTheMortal
     * @reason Route vanilla double plant tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        int plantType = func_149885_e(worldIn, x, y, z);

        if (plantType != 2 && plantType != 3) {
            return 16777215;
        }

        return BiomeColor.getGrassColorAtPos(worldIn, x, y, z);
    }
}
