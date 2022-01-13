package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Slice
{
    public long key;
    public long invalidationKey;

    public Slice prev;
    public Slice next;

    public AtomicInteger refCount = new AtomicInteger();

    public
    Slice()
    {
        this.markAsInvalid();
    }

    public abstract void invalidateRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
    public abstract void invalidateData();

    public final int
    getReferenceCount()
    {
        int result = refCount.get();

        return result;
    }

    public final int
    release()
    {
        int result = refCount.decrementAndGet();

        return result;
    }

    public final void
    acquire()
    {
        refCount.incrementAndGet();
    }

    public final void
    markAsInvalid()
    {
        key = ColorCaching.INVALID_CHUNK_KEY;
    }

    public final void
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
