package fionathemortal.betterbiomeblend;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ColorResolverCompatibility {

    public static final BiomeColorResolver GRASS_COLOR = (blockAccess, x, y, z) -> blockAccess
        .getBiomeGenForCoords(x, z)
        .getBiomeGrassColor(x, y, z);
    public static final BiomeColorResolver WATER_COLOR = (blockAccess, x, y, z) -> blockAccess
        .getBiomeGenForCoords(x, z)
        .getWaterColorMultiplier();
    public static final BiomeColorResolver FOLIAGE_COLOR = (blockAccess, x, y, z) -> blockAccess
        .getBiomeGenForCoords(x, z)
        .getBiomeFoliageColor(x, y, z);

    public static final Lock lock = new ReentrantLock();
    public static volatile Map<BiomeColorResolver, Integer> knownColorResolvers = createInitialColorResolvers();

    public static int nextColorID = BiomeColorType.LAST + 1;

    private static Map<BiomeColorResolver, Integer> createInitialColorResolvers() {
        Map<BiomeColorResolver, Integer> result = new HashMap<>();

        result.put(GRASS_COLOR, BiomeColorType.GRASS);
        result.put(WATER_COLOR, BiomeColorType.WATER);
        result.put(FOLIAGE_COLOR, BiomeColorType.FOLIAGE);

        return result;
    }

    public static int addNewColorResolver(BiomeColorResolver colorResolver) {
        Integer result;

        lock.lock();
        try {
            result = knownColorResolvers.get(colorResolver);

            if (result == null) {
                result = nextColorID++;

                Map<BiomeColorResolver, Integer> newKnownColorResolvers = new HashMap<>(knownColorResolvers);

                newKnownColorResolvers.put(colorResolver, result);

                knownColorResolvers = newKnownColorResolvers;
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    public static int getColorResolverID(BiomeColorResolver colorResolver) {
        Integer id = knownColorResolvers.get(colorResolver);

        if (id == null) {
            id = addNewColorResolver(colorResolver);
        }

        return id;
    }
}
