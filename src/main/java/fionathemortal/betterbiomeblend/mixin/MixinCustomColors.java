package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import net.optifine.BlockPosM;
import net.optifine.CustomColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CustomColors.class)
public class MixinCustomColors
{
    @Overwrite
    private static int
    getSmoothColorMultiplier(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos, CustomColors.IColorizer colorizer, BlockPosM blockPosM)
    {
        int x = blockPos.getX();
        int z = blockPos.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        int colorizerID = OptifineProxy.getColorizerID(colorizer);

        ThreadLocal<ColorChunk> threadLocal = BiomeColor.getThreadLocalGenericChunkWrapper(blockAccess);

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocal, chunkX, chunkZ, colorizerID);

        if (chunk == null)
        {
            ColorChunkCache cache = BiomeColor.getColorChunkCacheForIBlockAccess(blockAccess);

            chunk = OptifineProxy.getBlendedColorChunk(cache, blockAccess, colorizerID, chunkX, chunkZ, colorizer, blockState);

            BiomeColor.setThreadLocalChunk(threadLocal, chunk, cache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
