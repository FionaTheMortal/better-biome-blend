package fionathemortal.betterbiomeblend;

import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ColorChunkCache 
{
	public Lock lock;
	
	public Long2ObjectLinkedOpenHashMap<ColorChunk> hash;
	public Stack<ColorChunk>                        freeStack;
	
	public int  invalidationCounter;
	
	public static long
	getChunkKey(int chunkX, int chunkZ)
	{
		long result = 
			((long)chunkZ << 32) | 
			((long)chunkX & 0xFFFFFFFFL);
		
		return result;
	}
	
	public
	ColorChunkCache(int count)
	{
		hash      = new Long2ObjectLinkedOpenHashMap<ColorChunk>(count);
		freeStack = new Stack<ColorChunk>();
		
		for (int index = 0;
			index < count;
			++index)
		{
			freeStack.add(new ColorChunk());
		}

		lock = new ReentrantLock();
	}

	public void
	releaseChunkWithoutLock(ColorChunk chunk)
	{
		int refCount = chunk.release();
		
		if (refCount == 0)
		{
			freeStack.push(chunk);				
		}
	}

	public void
	releaseChunk(ColorChunk chunk)
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
	invalidateChunk(int chunkX, int chunkZ)
	{
		lock.lock();
		
		++invalidationCounter;
	
		long key = getChunkKey(chunkX, chunkZ);
		
		ColorChunk chunk = hash.remove(key);
		
		if (chunk != null)
		{
			releaseChunkWithoutLock(chunk);
			
			chunk.markAsInvalid();
		}
		
		lock.unlock();
	}
	
	public void
	invalidateNeighbourhood(int chunkX, int chunkZ)
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
				long key = getChunkKey(chunkX + x, chunkZ + z);
				
				ColorChunk chunk = hash.remove(key);
				
				if (chunk != null)
				{
					releaseChunkWithoutLock(chunk);
					
					chunk.markAsInvalid();
				}
			}
		}	
		
		lock.unlock();
	}
	
	public void
	invalidateAll()
	{
		lock.lock();
		
		++invalidationCounter;
		
		for (ColorChunk chunk : hash.values())
		{
			releaseChunkWithoutLock(chunk);
			
			chunk.markAsInvalid();
		}
		
		hash.clear();
		
		lock.unlock();
	}

	public ColorChunk
	getChunk(int chunkX, int chunkZ)
	{
		ColorChunk result;
		
		long key = getChunkKey(chunkX, chunkZ);
	
		lock.lock();
		
		result = hash.getAndMoveToFirst(key);
		
		if (result != null)
		{
			result.acquire();
		}
		
		lock.unlock();
		
		return result;
	}
	
	public void
	putChunk(ColorChunk chunk)
	{
		chunk.acquire();
		
		lock.lock();
		
		ColorChunk prev = hash.getAndMoveToFirst(chunk.key);
		
		if (prev != null)
		{
			ColorChunk olderChunk;
			
			if (chunk.invalidationCounter >= prev.invalidationCounter)
			{
				olderChunk = prev;
				
				hash.put(chunk.key, chunk);
			}
			else
			{
				olderChunk = chunk;
			}
			
			releaseChunkWithoutLock(olderChunk);
			
			olderChunk.markAsInvalid();
		}
		else
		{
			hash.putAndMoveToFirst(chunk.key, chunk);
		}
		
		lock.unlock();
	}
	
	public ColorChunk
	newChunk(int chunkX, int chunkZ)
	{
		ColorChunk result = null;
		
		long key = getChunkKey(chunkX, chunkZ);
		
		lock.lock();
		
		if (!freeStack.empty())
		{
			result = freeStack.pop();
		}
		else
		{
			for (;;)
			{
				result = hash.removeLast();
				
				if (result.refCount.get() == 1)
				{
					result.release();
					break;
				}
			}
		}
		
		lock.unlock();
		
		result.key = key;
		result.invalidationCounter = invalidationCounter;
		
		result.acquire();
		
		return result;
	}
}
