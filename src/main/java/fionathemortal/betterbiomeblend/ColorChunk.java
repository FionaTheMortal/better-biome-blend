package fionathemortal.betterbiomeblend;

import java.util.concurrent.atomic.AtomicInteger;

public class ColorChunk 
{
	public int[] data;
	
	public long key;
	public int  invalidationCounter;
	
	public AtomicInteger refCount = new AtomicInteger();
	
	public 
	ColorChunk()
	{
		this.data = new int[16 * 16];
		
		this.markAsInvalid();
	}
	
	public int
	release()
	{
		int result = refCount.decrementAndGet();
		
		return result;
	}
	
	public void
	acquire()
	{
		refCount.incrementAndGet();
	}
	
	public void
	markAsInvalid()
	{
		key = Long.MIN_VALUE;
	}
	
	public int
	getColor(int x, int z)
	{
		int blockX = x & 15;
		int blockZ = z & 15;
		
		int blockIndex = (blockZ << 4) | blockX;
		
		int result = this.data[blockIndex];
		
		return result;
	}
}
