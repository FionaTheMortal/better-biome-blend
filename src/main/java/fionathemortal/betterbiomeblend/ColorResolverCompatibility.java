package fionathemortal.betterbiomeblend;

import net.minecraft.world.biome.BiomeColorHelper;

import javax.swing.plaf.synth.ColorType;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ColorResolverCompatibility
{
    public static final Lock                                             lock                = new ReentrantLock();
    public static final HashMap<BiomeColorHelper.ColorResolver, Integer> knownColorResolvers = new HashMap<>();

    public static int nextColorID = BiomeColorType.LAST + 1;

    static
    {
        knownColorResolvers.put(BiomeColorHelper.GRASS_COLOR, BiomeColorType.GRASS);
        knownColorResolvers.put(BiomeColorHelper.WATER_COLOR, BiomeColorType.WATER);
        knownColorResolvers.put(BiomeColorHelper.FOLIAGE_COLOR, BiomeColorType.FOLIAGE);
    }

    public static int
    addNewColorResolver(BiomeColorHelper.ColorResolver colorResolver)
    {
        lock.lock();

        int id = nextColorID++;

        knownColorResolvers.put(colorResolver, id);

        lock.unlock();

        return id;
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
