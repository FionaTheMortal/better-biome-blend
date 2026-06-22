package fionathemortal.betterbiomeblend;

import net.minecraft.world.IBlockAccess;

public interface BiomeColorResolver {

    int getColorAtPos(IBlockAccess blockAccess, int x, int y, int z);
}
