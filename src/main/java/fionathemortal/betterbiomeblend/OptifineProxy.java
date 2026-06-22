package fionathemortal.betterbiomeblend;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.optifine.CustomColors;

import java.util.HashMap;
import java.util.Map;

public class OptifineProxy
{
    private static volatile Map<CustomColors.IColorizer, Integer> knownColorizers = new HashMap<>();

    private static int
    addNewColorizer(CustomColors.IColorizer colorizer)
    {
        Integer result;

        ColorResolverCompatibility.lock.lock();

        try
        {
            result = knownColorizers.get(colorizer);

            if (result == null)
            {
                result = ColorResolverCompatibility.nextColorID++;

                Map<CustomColors.IColorizer, Integer> newKnownColorizers = new HashMap<>(knownColorizers);

                newKnownColorizers.put(colorizer, result);

                knownColorizers = newKnownColorizers;
            }
        }
        finally
        {
            ColorResolverCompatibility.lock.unlock();
        }

        return result;
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
