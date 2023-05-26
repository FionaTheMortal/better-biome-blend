package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.ColorCaching;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.atomic.AtomicLong;
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
    releaseSlices(T[] slices)
    {
        lock.lock();

        for (int index = 0;
            index < 27;
            ++index)
        {
            T slice = slices[index];

            int refCount = slice.release();

            if (refCount == 0)
            {
                if (slice.size == this.sliceSize)
                {
                    freeListPush(slice);
                }
            }
        }

        lock.unlock();
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

    public static AtomicLong hit  = new AtomicLong();
    public static AtomicLong miss = new AtomicLong();

    public final void
    getOrDefaultInitializeNeighbors(T[] result, int sliceSize, int sliceX, int sliceY, int sliceZ, int colorType)
    {
        if (sliceSize == this.sliceSize)
        {
            lock.lock();

            int sliceIndex = 0;

            for (int offsetZ = -1;
                 offsetZ <= 1;
                 ++offsetZ)
            {
                for (int offsetY = -1;
                     offsetY <= 1;
                     ++offsetY)
                {
                    for (int offsetX = -1;
                         offsetX <= 1;
                         ++offsetX)
                    {
                        final int neighborSliceX = sliceX + offsetX;
                        final int neighborSliceY = sliceY + offsetY;
                        final int neighborSliceZ = sliceZ + offsetZ;

                        long key = ColorCaching.getChunkKey(neighborSliceX, neighborSliceY, neighborSliceZ, colorType);

                        T slice = hash.getAndMoveToFirst(key);

                        if (slice == null)
                        {
                            if (freeListEmpty())
                            {
                                for (;;)
                                {
                                    long lastKey = hash.lastLongKey();

                                    slice = hash.removeLast();

                                    if (slice.getReferenceCount() == 1)
                                    {
                                        slice.release();
                                        break;
                                    }
                                    else
                                    {
                                        hash.putAndMoveToFirst(lastKey, slice);
                                    }
                                }

                                removeFromInvalidationHash(slice);
                            }
                            else
                            {
                                slice = freeListPop();
                            }

                            long invalidationKey = ColorCaching.getChunkKey(sliceX, 0, sliceZ, 0);

                            slice.key = key;
                            slice.columnKey = invalidationKey;

                            slice.prev = null;
                            slice.next = null;

                            slice.invalidateData();

                            slice.acquire();

                            hash.putAndMoveToFirst(slice.key, slice);

                            addToInvalidationHash(slice);
                        }

                        slice.acquire();

                        result[sliceIndex++] = slice;
                    }
                }
            }

            lock.unlock();
        }
        else
        {
            for (int index = 0;
                index < 27;
                ++index)
            {
                result[index] = newSlice(sliceSize);
            }
        }
    }
}
