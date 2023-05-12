package fionathemortal.betterbiomeblend.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class BlendChunk
{
    public int[] data;
    public long  key;
    public int   invalidationCounter;

    public AtomicInteger refCount = new AtomicInteger();

    public long invalidationKey;

    BlendChunk prev;
    BlendChunk next;

    public
    BlendChunk()
    {
        this.data = new int[16 * 16 * 16];

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
