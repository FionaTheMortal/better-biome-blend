package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockVine;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockVine.class)
public abstract class MixinBlockVine {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla vine tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        return BiomeColor.getFoliageColorAtPos(worldIn, x, y, z);
    }
}
