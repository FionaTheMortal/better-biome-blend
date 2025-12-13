package fionathemortal.betterbiomeblend.common.cache.source;

import fionathemortal.betterbiomeblend.common.util.Utility;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Slice
{
    public long key;
    public int  size;
    public int  salt;
    public int  age;

    public AtomicInteger refCount = new AtomicInteger();

    public
    Slice(int size, int salt)
    {
        this.size = size;
        this.salt = salt;

        this.markAsInvalid();
    }

    public abstract void invalidateData();

    public final int
    getRefCount()
    {
        int result = refCount.get();

        return result;
    }

    public final void
    release()
    {
        refCount.decrementAndGet();
    }

    public final void
    acquire()
    {
        refCount.incrementAndGet();
    }

    public final boolean
    isInvalid()
    {
        boolean result = ((this.key ^ this.salt) == Utility.INVALID_CHUNK_KEY);

        return result;
    }

    public final void
    markAsInvalid()
    {
        key = Utility.INVALID_CHUNK_KEY ^ salt;
    }
}
