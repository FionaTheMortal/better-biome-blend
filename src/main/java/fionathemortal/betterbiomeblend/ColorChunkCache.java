package fionathemortal.betterbiomeblend;

import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public final class ColorChunkCache 
{
	public final Lock lock;
	
	public final Long2ObjectLinkedOpenHashMap<ColorChunk> hash;
	public final Stack<ColorChunk>                        freeStack;
	
	public int invalidationCounter;
	
	public static long
	getChunkKey(int chunkX, int chunkZ, int colorType)
	{
		/*
		long result = 
			((long)chunkZ << 32) | 
			((long)chunkX & 0xFFFFFFFFL);
		*/
		
		long result =
			((long)(chunkZ & 0x7FFFFFFFL) << 31) | 
			((long)(chunkX & 0x7FFFFFFFL))       |
			((long)colorType << 62);
				
		return result;
	}
	
	public
	ColorChunkCache(int count)
	{
		lock = new ReentrantLock();
		
		hash      = new Long2ObjectLinkedOpenHashMap<ColorChunk>(count);
		freeStack = new Stack<ColorChunk>();
		
		for (int index = 0;
			index < count;
			++index)
		{
			freeStack.add(new ColorChunk());
		}
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
	
		for (int colorType = BiomeColorType.FIRST;
			colorType <= BiomeColorType.LAST;
			++colorType)
		{
			long key = getChunkKey(chunkX, chunkZ, colorType);
			
			ColorChunk chunk = hash.remove(key);
			
			if (chunk != null)
			{
				releaseChunkWithoutLock(chunk);
				
				chunk.markAsInvalid();
			}
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
				for (int colorType = BiomeColorType.FIRST;
					colorType <= BiomeColorType.LAST;
					++colorType)
				{
					long key = getChunkKey(chunkX + x, chunkZ + z, colorType);
					
					ColorChunk chunk = hash.remove(key);
					
					if (chunk != null)
					{
						releaseChunkWithoutLock(chunk);
						
						chunk.markAsInvalid();
					}
				}
			}
		}	
		
		lock.unlock();
	}
	
	public void
	invalidateNeighbourhood2(int chunkX, int chunkZ)
	{
		lock.lock();
		
		++invalidationCounter;
		
		for (int index = 0;
			index < 9;
			++index)
		{
			int x = BetterBiomeBlendClient.chunkOffsets[2 * index + 0];
			int z = BetterBiomeBlendClient.chunkOffsets[2 * index + 1];

			if (x != 0 || z != 0)
			{
				for (int colorType = BiomeColorType.FIRST;
					colorType <= BiomeColorType.LAST;
					++colorType)
				{
					long key = getChunkKey(chunkX + x, chunkZ + z, colorType);
					
					ColorChunk chunk = hash.get(key);
					
					if (chunk != null)
					{
						int rectParamsOffset = 6 * index;
						
						int minX = BetterBiomeBlendClient.copyRectParams[rectParamsOffset + 0] * (16 - 2);
						int minZ = BetterBiomeBlendClient.copyRectParams[rectParamsOffset + 1] * (16 - 2);
						
						int maxX = BetterBiomeBlendClient.copyRectParams[rectParamsOffset + 2] * (2 - 16) + 16;
						int maxZ = BetterBiomeBlendClient.copyRectParams[rectParamsOffset + 3] * (2 - 16) + 16;

						for (int z1 = minZ;
							z1 < maxZ;
							++z1)
						{
							for (int x1 = minX;
								x1 < maxX;
								++x1)
							{
								chunk.data[3 * (16 * z1 + x1) + 0] = (byte)-1;
								chunk.data[3 * (16 * z1 + x1) + 1] = (byte)-1;
								chunk.data[3 * (16 * z1 + x1) + 2] = (byte)-1;
							}
						}
					}
				}
			}
		}	
		
		for (int colorType = BiomeColorType.FIRST;
			colorType <= BiomeColorType.LAST;
			++colorType)
		{
			long key = getChunkKey(chunkX, chunkZ, colorType);
			
			ColorChunk chunk = hash.remove(key);
			
			if (chunk != null)
			{
				releaseChunkWithoutLock(chunk);
				
				chunk.markAsInvalid();
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
	getChunk(int chunkX, int chunkZ, int colorType)
	{
		ColorChunk result;
		
		long key = getChunkKey(chunkX, chunkZ, colorType);
	
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
	newChunk(int chunkX, int chunkZ, int colorType)
	{
		ColorChunk result = null;
		
		long key = getChunkKey(chunkX, chunkZ, colorType);
		
		lock.lock();
		
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
		}
		
		lock.unlock();
		
		result.key = key;
		result.invalidationCounter = invalidationCounter;

		result.acquire();

		return result;
	}
}
