package fionathemortal.betterbiomeblend;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.optifine.CustomColors;
import scala.Int;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OptifineProxy
{
    private static final HashMap<CustomColors.IColorizer, Integer> knownColorizers = new HashMap<>();

    private static int
    addNewColorizer(CustomColors.IColorizer colorizer)
    {
        ColorResolverCompatibility.lock.lock();

        int id = ColorResolverCompatibility.nextColorID++;

        knownColorizers.put(colorizer, id);

        ColorResolverCompatibility.lock.unlock();

        return id;
    }

    public static int
    getColorizerID(CustomColors.IColorizer colorizer)
    {
        Integer id = knownColorizers.get(colorizer);

        if (id == null)
        {
            id = addNewColorizer(colorizer);
        }

        return id;
    }


    public static ColorChunk
    getBlendedColorChunk(
        ColorChunkCache         cache,
        IBlockAccess            blockAccess,
        int                     colorID,
        int                     chunkX,
        int                     chunkZ,
        CustomColors.IColorizer colorizer,
        IBlockState             blockState)
    {
        ColorChunk chunk = cache.getChunk(chunkX, chunkZ, colorID);

        if (chunk == null)
        {
            chunk = cache.newChunk(chunkX, chunkZ, colorID);

            BiomeColorHelper.ColorResolver colorResolver = new BiomeColorHelper.ColorResolver() {
                public int getColorAtPos(Biome biome, BlockPos blockPosition) {
                    return colorizer.getColor(blockState, blockAccess, blockPosition);
                }
            };

            BiomeColor.generateBlendedColorChunk(blockAccess, chunkX, chunkZ, chunk.data, colorID, colorResolver);

            cache.putChunk(chunk);
        }

        return chunk;
    }
}
