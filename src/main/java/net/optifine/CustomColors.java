package net.optifine;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class CustomColors
{
    private static int
    getSmoothColorMultiplier(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos, CustomColors.IColorizer colorizer, BlockPosM blockPosM)
    {
        return 0;
    }

    public interface IColorizer
    {
        int getColor(IBlockState var1, IBlockAccess var2, BlockPos var3);

        boolean isColorConstant();
    }
}
