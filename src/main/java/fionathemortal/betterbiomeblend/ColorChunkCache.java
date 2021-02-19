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
			synchronized(this)
			{
				freeStack.push(chunk);				
			}
		}
	}
	
	public void
	invalidateChunk(int chunkX, int chunkZ)
	{
		synchronized(this)
		{
			++invalidationCounter;
		
			long key = getChunkKey(chunkX, chunkZ);
			
			ColorChunk chunk = hash.remove(key);
			
			if (chunk != null)
			{
				releaseChunkWithoutLock(chunk);
				
				chunk.markAsInvalid();
			}
		}
	}
	
	public void
	invalidateNeighbourhood(int chunkX, int chunkZ)
	{
		synchronized(this)
		{
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
		}
	}
	
	public void
	invalidateAll()
	{
		synchronized(this)
		{
			++invalidationCounter;
			
			for (ColorChunk chunk : hash.values())
			{
				releaseChunkWithoutLock(chunk);
				
				chunk.markAsInvalid();
			}
			
			hash.clear();	
		}
	}

	public ColorChunk
	getChunk(int chunkX, int chunkZ)
	{
		ColorChunk result;
		
		long key = getChunkKey(chunkX, chunkZ);
	
		synchronized(this)
		{
			result = hash.getAndMoveToFirst(key);
			
			if (result != null)
			{
				result.acquire();
			}
		}
		
		return result;
	}
	
	public void
	putChunk(ColorChunk chunk)
	{
		chunk.acquire();
		
		synchronized(this)
		{
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
		}
	}
	
	public ColorChunk
	newChunk(int chunkX, int chunkZ)
	{
		ColorChunk result = null;
		
		long key = getChunkKey(chunkX, chunkZ);
		
		synchronized(this)
		{
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
		}
		
		result.key = key;
		result.invalidationCounter = invalidationCounter;
		
		result.acquire();
		
		return result;
	}
}
