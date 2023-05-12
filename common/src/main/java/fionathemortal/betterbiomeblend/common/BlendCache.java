package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.debug.Debug;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BlendCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BlendChunk> hash;
    public final Stack<BlendChunk>                        freeList;
    public final Long2ObjectOpenHashMap<BlendChunk>       invalidationHash;

    public int invalidationCounter = 0;

    public
    BlendCache(int count)
    {
        lock     = new ReentrantLock();
        hash     = new Long2ObjectLinkedOpenHashMap<>(count);
        freeList = new Stack<>();

        invalidationHash = new Long2ObjectOpenHashMap<>(count);

        for (int index = 0;
            index < count;
            ++index)
        {
            freeList.add(new BlendChunk());
        }
    }

    public void
    releaseChunkWithoutLock(BlendChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            freeList.push(chunk);
        }
    }

    public void
    releaseChunk(BlendChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            freeList.push(chunk);

            lock.unlock();
        }
    }

    public void
    addToInvalidationHash(BlendChunk chunk)
    {
        BlendChunk otherChunk = invalidationHash.get(chunk.invalidationKey);

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

    public void
    removeFromInvalidationHash(BlendChunk chunk)
    {
        if (chunk.prev == null)
        {
            invalidationHash.remove(chunk.invalidationKey);

            if (chunk.next != null)
            {
                invalidationHash.put(chunk.invalidationKey, chunk.next);
            }
        }

        chunk.removeFromLinkedList();
    }

    public void
    invalidateAll()
    {
        lock.lock();

        ++invalidationCounter;

        for (BlendChunk chunk : hash.values())
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

    public void
    invalidateChunk(int chunkX, int chunkZ)
    {
        lock.lock();

        ++invalidationCounter;

        for (int z = -1;
             z <= 1;
             ++z)
        {
            for (int x = -1;
                x <= 1;
                ++x)
            {
                long key = ColorCaching.getChunkKey(chunkX + x, 0, chunkZ + z, 0);

                BlendChunk first = invalidationHash.get(key);

                for (BlendChunk current = first;
                     current != null;
                    )
                {
                    BlendChunk next = current.next;

                    hash.remove(current.key);

                    removeFromInvalidationHash(current);

                    releaseChunkWithoutLock(current);

                    current.markAsInvalid();

                    current = next;
                }
            }
        }

        lock.unlock();
    }

    public BlendChunk
    getOrInitChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

        lock.lock();

        BlendChunk result = hash.getAndMoveToFirst(key);

        Debug.countBlendCache(result);

        if (result == null)
        {
            if (!freeList.empty())
            {
                result = freeList.pop();
            }
            else
            {
                for (;;)
                {
                    result = hash.removeLast();

                    if (result.getReferenceCount() == 1)
                    {
                        result.release();
                        break;
                    }
                    else
                    {
                        hash.putAndMoveToFirst(result.key, result);
                    }
                }

                removeFromInvalidationHash(result);
            }

            long invalidationKey = ColorCaching.getChunkKey(chunkX, 0, chunkZ, 0);

            result.key = key;
            result.invalidationCounter = invalidationCounter;
            result.invalidationKey = invalidationKey;

            result.prev = null;
            result.next = null;

            Arrays.fill(result.data, 0);

            hash.putAndMoveToFirst(result.key, result);

            addToInvalidationHash(result);

            result.acquire();
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
