package fionathemortal.betterbiomeblend.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class ColorChunk
{
    public byte[] data;
    public long   key;
    public int    invalidationCounter;

    public AtomicInteger refCount = new AtomicInteger();

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
}
