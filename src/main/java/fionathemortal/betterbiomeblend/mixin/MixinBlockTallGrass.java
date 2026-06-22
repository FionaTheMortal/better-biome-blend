package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.BlockTallGrass;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockTallGrass.class)
public abstract class MixinBlockTallGrass {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla tallgrass tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        int meta = worldIn.getBlockMetadata(x, y, z);

        if (meta == 0) {
            return 16777215;
        }

        return BiomeColor.getGrassColorAtPos(worldIn, x, y, z);
    }
}
