package fionathemortal.betterbiomeblend.common.compat;

import fionathemortal.betterbiomeblend.common.BiomeColorType;
import net.minecraft.world.level.ColorResolver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class CustomColorResolverCompatibility
{
    public static final ReentrantLock                             lock                = new ReentrantLock();
    public static final ConcurrentHashMap<ColorResolver, Integer> knownColorResolvers = new ConcurrentHashMap<>();

    public static int nextColorResolverID = BiomeColorType.LAST + 1;

    public static int
    addNewColorResolverID()
    {
        int result = nextColorResolverID++;

        return result;
    }

    public static int
    addNewColorResolver(ColorResolver resolver)
    {
        lock.lock();

        int result;

        if (!knownColorResolvers.contains(resolver))
        {
            result = addNewColorResolverID();

            knownColorResolvers.put(resolver, result);
        }
        else
        {
            result = knownColorResolvers.get(resolver);
        }

        lock.unlock();

        return result;
    }

    public static int
    getColorType(ColorResolver resolver)
    {
        Integer result = knownColorResolvers.get(resolver);

        if (result == null)
        {
            result = addNewColorResolver(resolver);
        }

        return result;
    }
}
