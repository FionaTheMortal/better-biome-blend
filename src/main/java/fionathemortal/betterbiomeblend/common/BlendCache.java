package fionathemortal.betterbiomeblend.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BlendCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BlendChunk> hash;
    public final Stack<BlendChunk>                        freeStack;
    public final ArrayList<BlendChunk>                    generating;
    public final Long2ObjectOpenHashMap<BlendChunk>       invalidationHash;

    public int invalidationCounter = 0;

    public
    BlendCache(int count)
    {
        lock       = new ReentrantLock();
        hash       = new Long2ObjectLinkedOpenHashMap<>(count);
        freeStack  = new Stack<>();
        generating = new ArrayList<>();

        invalidationHash = new Long2ObjectOpenHashMap<>(count);

        for (int index = 0;
            index < count;
            ++index)
        {
            freeStack.add(new BlendChunk());
        }
    }

    public void
    releaseChunkWithoutLock(BlendChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            freeStack.push(chunk);
        }
    }

    public void
    releaseChunk(BlendChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            freeStack.push(chunk);

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

        for (int x = -1;
            x <= 1;
            ++x)
        {
            for (int z = -1;
                z <= 1;
                ++z)
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

                ListIterator<BlendChunk> iterator = generating.listIterator();

                while (iterator.hasNext())
                {
                    BlendChunk generatingChunk = iterator.next();

                    if (generatingChunk.invalidationKey == key)
                    {
                        generatingChunk.markAsInvalid();

                        iterator.remove();
                    }
                }
            }
        }

        lock.unlock();
    }

    public BlendChunk
    getChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

        lock.lock();

        BlendChunk result = hash.getAndMoveToFirst(key);

        if (result != null)
        {
            result.acquire();
        }

        lock.unlock();

        return result;
    }

    public BlendChunk
    newChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

        lock.lock();

        BlendChunk result = null;

        if (!freeStack.empty())
        {
            result = freeStack.pop();
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
        result.invalidationCounter = invalidationCounter;
        result.invalidationKey = invalidationKey;

        result.prev = null;
        result.next = null;

        result.acquire();

        generating.add(result);

        lock.unlock();

        return result;
    }

    public BlendChunk
    putChunk(BlendChunk chunk)
    {
        BlendChunk result = chunk;

        lock.lock();

        if (generating.remove(chunk))
        {
            BlendChunk prev = hash.getAndMoveToFirst(chunk.key);

            if (prev == null)
            {
                hash.putAndMoveToFirst(chunk.key, chunk);

                addToInvalidationHash(chunk);

                chunk.acquire();
            }
            else
            {
                BlendChunk olderChunk;

                if (chunk.invalidationCounter >= prev.invalidationCounter)
                {
                    olderChunk = prev;

                    hash.put(chunk.key, chunk);

                    addToInvalidationHash(chunk);
                    removeFromInvalidationHash(olderChunk);

                    chunk.acquire();
                }
                else
                {
                    olderChunk = chunk;

                    result = prev;
                    result.acquire();
                }

                releaseChunkWithoutLock(olderChunk);

                olderChunk.markAsInvalid();
            }
        }

        lock.unlock();

        return result;
    }
}
