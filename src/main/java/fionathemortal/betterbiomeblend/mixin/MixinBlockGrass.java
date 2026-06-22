package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockGrass;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockGrass.class)
public abstract class MixinBlockGrass {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla grass tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        return BiomeColor.getGrassColorAtPos(worldIn, x, y, z);
    }
}
