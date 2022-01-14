package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SliceCache<T extends Slice>
{
    public final ReentrantLock                   lock;
    public final Long2ObjectLinkedOpenHashMap<T> hash;
    public final Long2ObjectOpenHashMap<T>       invalidationHash;
    public       T                               firstFree;

    private static final byte[]
    invalidatedRectParams =
    {
        3, 4,
        0, 4,
        0, 0
    };

    public static int
    getInvalidatedRectMin(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidatedRectParams[offset + 0];

        return result;
    }

    public static int
    getInvalidatedRectMax(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidatedRectParams[offset + 1];

        return result;
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

    public
    SliceCache(int count, Supplier<T> supplier)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);

        invalidationHash = new Long2ObjectOpenHashMap<>(count);

        for (int index = 0;
             index < count;
             ++index)
        {
            T slice = supplier.get();

            freeListPush(slice);
        }
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
    releaseChunkWithoutLocking(T chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            freeListPush(chunk);
        }
    }

    public final void
    releaseChunk(T chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            freeListPush(chunk);

            lock.unlock();
        }
    }

    public final void
    invalidateAll()
    {
        lock.lock();

        for (T chunk : hash.values())
        {
            releaseChunkWithoutLocking(chunk);

            chunk.markAsInvalid();
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

    public final void
    invalidateSmallNeighborhood(int chunkX, int chunkZ)
    {
        lock.lock();

        for (int z = -1;
             z <= 0;
             ++z)
        {
            for (int x = -1;
                 x <= 0;
                 ++x)
            {
                long key = ColorCaching.getChunkKey(chunkX + x, 0, chunkZ + z, 0);

                T first = invalidationHash.get(key);

                for (T current = first;
                     current != null;
                    )
                {
                    T next = (T)current.next;

                    if (next == first)
                    {
                        int i = 0;
                    }

                    if (x == 0 && z == 0)
                    {
                        hash.remove(current.key);

                        removeFromInvalidationHash(current);

                        releaseChunkWithoutLocking(current);

                        current.markAsInvalid();
                    }
                    else
                    {
                        int minX = getInvalidatedRectMin(x);
                        int minY = 0;
                        int minZ = getInvalidatedRectMin(z);

                        int maxX = getInvalidatedRectMax(x);
                        int maxY = 4;
                        int maxZ = getInvalidatedRectMax(z);

                        current.invalidateRegion(minX, minY, minZ, maxX, maxY, maxZ);
                    }

                    current = next;
                }
            }
        }

        lock.unlock();
    }

    public final T
    getOrDefaultInitializeChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

        lock.lock();

        T result = hash.getAndMoveToFirst(key);

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

            long invalidationKey = ColorCaching.getChunkKey(chunkX, 0, chunkZ, 0);

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

        return result;
    }
}
