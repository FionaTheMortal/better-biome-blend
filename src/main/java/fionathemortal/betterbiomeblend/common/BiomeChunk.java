package fionathemortal.betterbiomeblend.common;

import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.atomic.AtomicInteger;

public final class BiomeChunk
{
    public Biome[] data;
    public long    key;

    public AtomicInteger refCount = new AtomicInteger();

    public long   invalidationKey;

    public BiomeChunk prev;
    public BiomeChunk next;

    public
    BiomeChunk()
    {
        this.data = new Biome[4 * 4 * 4];

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
        key = ColorCaching.INVALID_CHUNK_KEY;
    }

    public void
    removeFromLinkedList()
    {
        if (this.prev != null)
        {
            this.prev.next = this.next;
        }

        if (this.next != null)
        {
            this.next.prev = this.prev;
        }

        this.prev = null;
        this.next = null;
    }
}
