package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockReed;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockReed.class)
public abstract class MixinBlockReed {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla sugar cane tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        return BiomeColor.getGrassColorAtPos(worldIn, x, y, z);
    }
}
