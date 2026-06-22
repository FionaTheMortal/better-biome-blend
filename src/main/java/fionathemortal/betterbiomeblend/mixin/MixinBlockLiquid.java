package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fionathemortal.betterbiomeblend.BiomeColor;

@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

    /**
     * @author FionaTheMortal
     * @reason Route vanilla water tinting through Better Biome Blend's cached blender.
     */
    @Overwrite
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        if (((Block) (Object) this).getMaterial() != Material.water) {
            return 16777215;
        }

        return BiomeColor.getWaterColorAtPos(worldIn, x, y, z);
    }
}
