package fionathemortal.betterbiomeblend;

import java.util.concurrent.atomic.AtomicInteger;

public class BlendedColorChunk 
{
	public int[] data;
	
	public long    key;
	public int     index;
	public int     invalidationCounter;
	
	public AtomicInteger refCount = new AtomicInteger();
	
	public 
	BlendedColorChunk()
	{
		this.data = new int[16 * 16];
		this.makeInvalid();
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
	makeInvalid()
	{
		key = Long.MIN_VALUE;
	}
}
