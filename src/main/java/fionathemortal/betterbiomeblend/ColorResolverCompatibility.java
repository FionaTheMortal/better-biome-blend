package fionathemortal.betterbiomeblend;

import net.minecraft.world.biome.BiomeColorHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ColorResolverCompatibility
{
    public static final Lock                                            lock                = new ReentrantLock();
    public static volatile Map<BiomeColorHelper.ColorResolver, Integer> knownColorResolvers = createInitialColorResolvers();

    public static int nextColorID = BiomeColorType.LAST + 1;

    private static Map<BiomeColorHelper.ColorResolver, Integer>
    createInitialColorResolvers()
    {
        Map<BiomeColorHelper.ColorResolver, Integer> result = new HashMap<>();

        result.put(BiomeColorHelper.GRASS_COLOR, BiomeColorType.GRASS);
        result.put(BiomeColorHelper.WATER_COLOR, BiomeColorType.WATER);
        result.put(BiomeColorHelper.FOLIAGE_COLOR, BiomeColorType.FOLIAGE);

        return result;
    }

    public static int
    addNewColorResolver(BiomeColorHelper.ColorResolver colorResolver)
    {
        Integer result;

        lock.lock();
        try
        {
            result = knownColorResolvers.get(colorResolver);

            if (result == null)
            {
                result = nextColorID++;

                Map<BiomeColorHelper.ColorResolver, Integer> newKnownColorResolvers = new HashMap<>(knownColorResolvers);

                newKnownColorResolvers.put(colorResolver, result);

                knownColorResolvers = newKnownColorResolvers;
            }
        }
        finally
        {
            lock.unlock();
        }

        return result;
    }

    public static int
    getColorResolverID(BiomeColorHelper.ColorResolver colorResolver)
    {
        Integer id = knownColorResolvers.get(colorResolver);

        if (id == null)
        {
            id = addNewColorResolver(colorResolver);
        }

        return id;
    }
}
