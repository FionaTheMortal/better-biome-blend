package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockLeaves;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockLeaves.class)
public abstract class MixinBlockLeaves {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla leaves tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        return BiomeColor.getFoliageColorAtPos(worldIn, x, y, z);
    }
}
