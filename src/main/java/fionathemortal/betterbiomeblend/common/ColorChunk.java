package fionathemortal.betterbiomeblend.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class ColorChunk
{
    public byte[] data;
    public long   key;
    public long   invalidationKey;

    public AtomicInteger refCount = new AtomicInteger();

    ColorChunk prev;
    ColorChunk next;

    public
    ColorChunk()
    {
        this.data = new byte[4 * 4 * 4 * 3];

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
