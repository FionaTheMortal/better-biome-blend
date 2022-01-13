package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SliceCache<T extends Slice>
{
    private static final byte[]
    invalidationRectParams =
    {
        3, 4,
        0, 4,
        0, 0
    };

    public final ReentrantLock                   lock;
    public final Long2ObjectLinkedOpenHashMap<T> hash;
    public final Stack<T>                        free;
    public final Long2ObjectLinkedOpenHashMap<T> invalidationHash;

    public static int
    getInvalidationRectMin(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidationRectParams[offset + 0];

        return result;
    }

    public static int
    getInvalidationRectMax(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidationRectParams[offset + 1];

        return result;
    }

    public
    SliceCache(int count, Supplier<T> supplier)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        invalidationHash = new Long2ObjectLinkedOpenHashMap<>(count);

        for (int index = 0;
             index < count;
             ++index)
        {
            free.add(supplier.get());
        }
    }

    public final void
    releaseChunkWithoutLock(T chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            free.push(chunk);
        }
    }

    public final void
    releaseChunk(T chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            free.push(chunk);

            lock.unlock();
        }
    }

    public final void
    invalidateAll()
    {
        lock.lock();

        for (T chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

            chunk.prev = null;
            chunk.next = null;

            chunk.markAsInvalid();
        }

        hash.clear();

        invalidationHash.clear();

        lock.unlock();
    }

    public final void
    addToInvalidationHash(T chunk)
    {
        T otherChunk = invalidationHash.get(chunk.invalidationKey);

        if (otherChunk != null)
        {
            chunk.next = otherChunk.next;
            chunk.prev = otherChunk;

            if (otherChunk.next != null)
            {
                otherChunk.next.prev = chunk;
            }

            otherChunk.next = chunk;
        }
        else
        {
            invalidationHash.put(chunk.invalidationKey, chunk);
        }
    }

    public final void
    removeFromInvalidationHash(T chunk)
    {
        if (chunk.prev == null)
        {
            invalidationHash.remove(chunk.invalidationKey);

            if (chunk.next != null)
            {
                invalidationHash.put(chunk.invalidationKey, (T)chunk.next);
            }
        }

        chunk.removeFromLinkedList();
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

                        releaseChunkWithoutLock(current);

                        current.markAsInvalid();
                    }
                    else
                    {
                        int minX = getInvalidationRectMin(x);
                        int minY = 0;
                        int minZ = getInvalidationRectMin(z);

                        int maxX = getInvalidationRectMax(x);
                        int maxY = 4;
                        int maxZ = getInvalidationRectMax(z);

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
            if (!free.empty())
            {
                result = free.pop();
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
            result.invalidationKey = invalidationKey;

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
