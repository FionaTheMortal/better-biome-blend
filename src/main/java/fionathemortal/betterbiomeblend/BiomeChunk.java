package fionathemortal.betterbiomeblend;

import net.minecraft.world.biome.Biome;

import java.util.concurrent.atomic.AtomicInteger;

public final class BiomeChunk
{
    public Biome[] data;

    public long key;
    public int  invalidationCounter;

    public AtomicInteger refCount = new AtomicInteger();

    public
    BiomeChunk()
    {
        this.data = new Biome[16 * 16];

        this.markAsInvalid();
    }

    public int
    getReferenceCount()
    {
        int result = refCount.get();

        return result;
    }

    public int
    release()
    {
        int result = refCount.decrementAndGet();

        return result;
    }

    public void
    acquire()
    {
        refCount.incrementAndGet();
    }

    public void
    markAsInvalid()
    {
        key = -1;
    }
}
