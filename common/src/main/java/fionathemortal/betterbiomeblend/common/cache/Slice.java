package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.util.Array3i;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public final class Slice
{
    public static final int INVALID_COLOR = 0;

    public final int[] blockColors;
    public int         solidColor;

    public long key;
    public long ticket;

    Slice prev;
    Slice next;

    private final AtomicInteger refCount = new AtomicInteger();

    private final int dimH;
    private final int dimV;

    public final CacheConfig config;

    public
    Slice(CacheConfig config, int dimH, int dimV, boolean solid)
    {
        this.config = config;

        this.dimH = dimH;
        this.dimV = dimV;

        if (solid)
        {
            this.blockColors = null;
        }
        else
        {
            this.blockColors = new int[dimH * dimH * dimV];
        }

        key = SliceCache.INVALID_KEY;
    }

    public void
    init(long key)
    {
        this.key = key;
        this.solidColor = 0;

        if (blockColors != null)
        {
            Arrays.fill(blockColors, INVALID_COLOR);
        }

        this.ticket = 0;
    }

    public long
    getInvalidationKey()
    {
        return SliceCache.getInvalidationKey(key);
    }

    public boolean
    isSolid()
    {
        return (this.blockColors == null);
    }

    public int
    getColor(int blockX, int blockY, int blockZ)
    {
        int result;

        if (isSolid())
        {
            result = solidColor;
        }
        else
        {
            int index = Array3i.getArrayIndex(dimH, dimV, blockX, blockY, blockZ);

            result = blockColors[index];
        }

        return result;
    }

    public void
    setColor(int x, int y, int z, int color)
    {
        if (isSolid())
        {
            solidColor = color;
        }
        else
        {
            int index = Array3i.getArrayIndex(dimH, dimV, x, y, z);

            blockColors[index] = 0;
        }
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
        key = SliceCache.INVALID_KEY;
    }

    public void
    linkRemove()
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

    public void
    linkInsert(Slice next)
    {
        if (next != null)
        {
            next.prev = this;
            next.next = this.next;

            if (this.next != null)
            {
                this.next.prev = next;
            }

            this.next = next;
        }
    }
}
