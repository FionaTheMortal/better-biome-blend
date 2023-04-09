package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.ColorCaching;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.concurrent.locks.ReentrantLock;

public abstract class SliceCache<T extends Slice>
{
    public final ReentrantLock                   lock;

    public final Long2ObjectLinkedOpenHashMap<T> hash;
    public final Long2ObjectOpenHashMap<T>       invalidationHash;
    public       T                               firstFree;

    public final int                             sliceCount;
    public       int                             sliceSize;

    public abstract T newSlice(int sliceSize);

    public
    SliceCache(int count)
    {
        lock             = new ReentrantLock();
        hash             = new Long2ObjectLinkedOpenHashMap<>(count);
        invalidationHash = new Long2ObjectOpenHashMap<>(count);

        sliceCount       = count;

        allocateSlices(sliceSize);
    }

    public final void
    allocateSlices(int sliceSize)
    {
        this.sliceSize  = sliceSize;

        for (int index = 0;
             index < this.sliceCount;
             ++index)
        {
            T slice = newSlice(sliceSize);

            freeListPush(slice);
        }
    }

    public static void
    linkedListUnlink(Slice slice)
    {
        if (slice.prev != null)
        {
            slice.prev.next = slice.next;
        }

        if (slice.next != null)
        {
            slice.next.prev = slice.prev;
        }

        slice.prev = null;
        slice.next = null;
    }

    public static void
    linkedListLinkAfter(Slice list, Slice slice)
    {
        slice.next = list.next;
        slice.prev = list;

        if (list.next != null)
        {
            list.next.prev = slice;
        }

        list.next = slice;
    }

    public final void
    clearFreeList()
    {
        this.firstFree = null;
    }

    public boolean
    freeListEmpty()
    {
        boolean result = (this.firstFree == null);

        return result;
    }

    public void
    freeListPush(T slice)
    {
        slice.prev = null;
        slice.next = this.firstFree;

        if (this.firstFree != null)
        {
            this.firstFree.prev = slice;
        }

        this.firstFree = slice;
    }

    public T
    freeListPop()
    {
        T result = this.firstFree;

        if (result != null)
        {
            this.firstFree = (T)result.next;

            if (this.firstFree != null)
            {
                this.firstFree.prev = null;
            }

            result.next = null;
        }

        return result;
    }

    public final void
    releaseSlice(T slice)
    {
        int refCount = slice.release();

        if (refCount == 0)
        {
            lock.lock();

            if (slice.size == this.sliceSize)
            {
                freeListPush(slice);
            }

            lock.unlock();
        }
    }

    public final void
    invalidateAll(int blendRadius)
    {
        lock.lock();

        for (T chunk : hash.values())
        {
            int refCount = chunk.release();

            if (refCount == 0)
            {
                freeListPush(chunk);
            }

            chunk.markAsInvalid();
        }

        int sliceSize = BlendConfig.getSliceSize(blendRadius);

        if (sliceSize != this.sliceSize)
        {
            clearFreeList();

            allocateSlices(sliceSize);
        }

        hash.clear();

        invalidationHash.clear();

        lock.unlock();
    }

    public final void
    addToInvalidationHash(T chunk)
    {
        T otherChunk = invalidationHash.get(chunk.columnKey);

        if (otherChunk != null)
        {
            linkedListLinkAfter(otherChunk, chunk);
        }
        else
        {
            invalidationHash.put(chunk.columnKey, chunk);
        }
    }

    public final void
    removeFromInvalidationHash(T chunk)
    {
        if (chunk.prev == null)
        {
            invalidationHash.remove(chunk.columnKey);

            if (chunk.next != null)
            {
                invalidationHash.put(chunk.columnKey, (T)chunk.next);
            }
        }

        linkedListUnlink(chunk);
    }

    public final T
    getOrDefaultInitializeSlice(int blendRadius, int sliceX, int sliceY, int sliceZ, int colorType)
    {
        T result;

        int sliceSize = BlendConfig.getSliceSize(blendRadius);

        if (sliceSize == this.sliceSize)
        {
            long key = ColorCaching.getChunkKey(sliceX, sliceY, sliceZ, colorType);

            lock.lock();

            result = hash.getAndMoveToFirst(key);

            if (result == null)
            {
                if (!freeListEmpty())
                {
                    result = freeListPop();
                }
                else
                {
                    for (;;)
                    {
                        long lastKey = hash.lastLongKey();

                        result = hash.removeLast();

                        if (result.getReferenceCount() == 1)
                        {
                            result.release();
                            break;
                        }
                        else
                        {
                            hash.putAndMoveToFirst(lastKey, result);
                        }
                    }

                    removeFromInvalidationHash(result);
                }

                long invalidationKey = ColorCaching.getChunkKey(sliceX, 0, sliceZ, 0);

                result.key = key;
                result.columnKey = invalidationKey;

                result.prev = null;
                result.next = null;

                result.invalidateData();

                result.acquire();

                hash.putAndMoveToFirst(result.key, result);

                addToInvalidationHash(result);
            }

            result.acquire();

            lock.unlock();
        }
        else
        {
            // TODO: Actually init chunk. Is this how it should be done ?

            result = newSlice(sliceSize);
        }

        return result;
    }
}
