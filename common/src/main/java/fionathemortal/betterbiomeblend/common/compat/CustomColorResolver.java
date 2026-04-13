package fionathemortal.betterbiomeblend.common.compat;

import fionathemortal.betterbiomeblend.common.ColorType;
import net.minecraft.world.level.ColorResolver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class CustomColorResolver
{
    public static final ReentrantLock                             lock                = new ReentrantLock();
    public static final ConcurrentHashMap<ColorResolver, Integer> knownColorResolvers = new ConcurrentHashMap<>();

    public static int nextColorResolverID = ColorType.LAST + 1;

    public static int
    addNewColorResolverID()
    {
        return nextColorResolverID++;
    }

    public static int
    addNewColorResolver(ColorResolver resolver)
    {
        lock.lock();

        if (!knownColorResolvers.containsKey(resolver))
        {
            int newID = addNewColorResolverID();

            knownColorResolvers.put(resolver, newID);
        }

        int result = knownColorResolvers.get(resolver);

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
