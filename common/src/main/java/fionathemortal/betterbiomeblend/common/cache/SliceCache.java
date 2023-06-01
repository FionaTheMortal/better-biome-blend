package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.ColorCaching;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public abstract class SliceCache<T extends Slice>
{
    public final ReentrantLock                   lock;

    public final Long2ObjectLinkedOpenHashMap<T> hash;
    public       T                               firstFree;

    public final int                             sliceCount;
    public       int                             sliceSize;

    public abstract T newSlice(int sliceSize);

    public
    SliceCache(int count)
    {
        lock       = new ReentrantLock(true);
        hash       = new Long2ObjectLinkedOpenHashMap<>(count);
        sliceCount = count;

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

        lock.unlock();
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
                                    slice = hash.removeLast();

                                    if (slice.getReferenceCount() == 1)
                                    {
                                        slice.release();
                                        break;
                                    }
                                    else
                                    {
                                        hash.putAndMoveToFirst(slice.key, slice);
                                    }
                                }
                            }
                            else
                            {
                                slice = freeListPop();
                            }

                            slice.key = key;

                            slice.prev = null;
                            slice.next = null;

                            slice.invalidateData();

                            slice.acquire();

                            hash.putAndMoveToFirst(slice.key, slice);
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
