package fionathemortal.betterbiomeblend;

import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.level.ColorResolver;

public final class BiomeColor 
{
	public static final Lock                   freeBlendCacheslock = new ReentrantLock();
	public static final Stack<ColorBlendCache> freeBlendCaches     = new Stack<>();

	public static final byte[]
	neighbourOffsets = 
	{
		-1, -1,
		 0, -1,
		 1, -1,
		-1,  0,
		 0,  0,
		 1,  0,
		-1,  1,
		 0,  1,
		 1,  1
	};

	public static final byte[]
	neighbourRectParams = 
	{
	 	-1, -1,  0,  0, -16, -16,  0,  0,
		 0, -1,  0,  0,   0, -16,  0,  0,
		 0, -1, -1,  0,  16, -16,  0,  0,
		-1,  0,  0,  0, -16,   0,  0,  0,
		 0,  0,  0,  0,   0,   0,  0,  0,
		 0,  0, -1,  0,  16,   0,  0,  0,
		-1,  0,  0, -1, -16,  16,  0,  0,
		 0,  0,  0, -1,   0,  16,  0,  0,
		 0,  0, -1, -1,  16,  16,  0,  0
	};
	
	public static int
	getNeighbourOffsetX(int chunkIndex)
	{
		int result = neighbourOffsets[2 * chunkIndex + 0];
		
		return result;
	}
	
	public static int
	getNeighbourOffsetZ(int chunkIndex)
	{
		int result = neighbourOffsets[2 * chunkIndex + 1];
		
		return result;
	}
	
	public static int
	getNeighbourRectMinX(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = neighbourRectParams[offset + 0] & (16 - radius);

		return result;
	}
	
	public static int
	getNeighbourRectMinZ(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = neighbourRectParams[offset + 1] & (16 - radius);
		
		return result;
	}
	
	public static int
	getNeighbourRectMaxX(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = (neighbourRectParams[offset + 2] & (radius - 16)) + 16;
		
		return result;
	}
	
	public static int
	getNeighbourRectMaxZ(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = (neighbourRectParams[offset + 3] & (radius - 16)) + 16;
		
		return result;
	}
	
	public static int
	getNeighbourRectBlendCacheMinX(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = Math.max(neighbourRectParams[offset + 4] + radius, 0);
	
		return result;
	}
	
	public static int
	getNeighbourRectBlendCacheMinZ(int chunkIndex, int radius)
	{
		int offset = 8 * chunkIndex;
		int result = Math.max(neighbourRectParams[offset + 5] + radius, 0);

		return result;
	}
	
	
	public static void
	clearBlendCaches()
	{
		freeBlendCacheslock.lock();
		
		freeBlendCaches.clear();
		
		freeBlendCacheslock.unlock();
	}
	
	public static ColorBlendCache
	acquireBlendCache(int blendRadius)
	{
		ColorBlendCache result = null;
		
		freeBlendCacheslock.lock();
		
		while (!freeBlendCaches.empty())
		{
			result = freeBlendCaches.pop();
			
			if (result.blendRadius == blendRadius)
			{
				break;
			}
		}
				
		freeBlendCacheslock.unlock();
		
		if (result == null)
		{
			result = new ColorBlendCache(blendRadius);
		}
		
		return result;
	}
	
	public static void
	releaseBlendCache(ColorBlendCache cache)
	{
		freeBlendCacheslock.lock();
		
		int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();
		
		if (cache.blendRadius == blendRadius)
		{
			freeBlendCaches.push(cache);
		}
		
		freeBlendCacheslock.unlock();		
	}
		
	public static ColorChunk
	getThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, int chunkX, int chunkZ, int colorType)
	{
		ColorChunk result = null;
		ColorChunk local = threadLocal.get();
		
		long key = ColorChunkCache.getChunkKey(chunkX, chunkZ, colorType);
		
		if (local.key == key)
		{
			result = local;
		}
		
		return result;
	}
	
	public static void
	setThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, ColorChunk chunk, ColorChunkCache cache)
	{
		ColorChunk local = threadLocal.get();
		
		cache.releaseChunk(local);
		
		threadLocal.set(chunk);
	}
	
	public static void
	gatherRawColorsForChunk(World world, byte[] result, int chunkX, int chunkZ, ColorResolver colorResolver)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = 16 * chunkX;
		int blockZ = 16 * chunkZ;

		int dstIndex = 0;

		double baseXF64 = (double)blockX;
		double baseZF64 = (double)blockZ;
		
		double zF64 = baseZF64;
		
		for (int z = 0;
			z < 16;
			++z)
		{
			double xF64 = baseXF64;
			
			for (int x = 0;
				x < 16;
				++x)
			{
				blockPos.setPos(blockX + x, 0, blockZ + z);
				
				int color = colorResolver.getColor(world.getBiome(blockPos), xF64, zF64);
				
				int colorR = Color.RGBAGetR(color);
				int colorG = Color.RGBAGetG(color);
				int colorB = Color.RGBAGetB(color);
				
				result[3 * dstIndex + 0] = (byte)colorR;
				result[3 * dstIndex + 1] = (byte)colorG;
				result[3 * dstIndex + 2] = (byte)colorB;

				++dstIndex;

				xF64 += 1.0;
			}
			
			zF64 += 1.0;
		}
	}
		
	public static void
	gatherRawColorsForRect(
		World  world,
		int    chunkX,
		int    chunkZ,
		int    minX,
		int    maxX,
		int    minZ,
		int    maxZ,
		byte[] result, 
		ColorResolver colorResolver)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = 16 * chunkX;
		int blockZ = 16 * chunkZ;

		double baseXF64 = (double)(blockX + minX);
		double baseZF64 = (double)(blockZ + minZ);
		
		double zF64 = baseZF64;
		
		for (int z = minZ;
			z < maxZ;
			++z)
		{
			double xF64 = baseXF64;

			for (int x = minX;
				x < maxX;
				++x)
			{
				int currentR = 0xFF & result[3 * (16 * z + x) + 0];
				int currentG = 0xFF & result[3 * (16 * z + x) + 1];
				int currentB = 0xFF & result[3 * (16 * z + x) + 2];

				int commonBits = currentR & currentG & currentB;
				
				if (commonBits == 0xFF)
				{
					blockPos.setPos(blockX + x, 0, blockZ + z);
					
					int color = colorResolver.getColor(world.getBiome(blockPos), xF64, zF64);
					
					int colorR = Color.RGBAGetR(color);
					int colorG = Color.RGBAGetG(color);
					int colorB = Color.RGBAGetB(color);
					
					result[3 * (16 * z + x) + 0] = (byte)colorR;
					result[3 * (16 * z + x) + 1] = (byte)colorG;
					result[3 * (16 * z + x) + 2] = (byte)colorB;
				}
				
				xF64 += 1.0;
			}
			
			zF64 += 1.0;
		}
	}
	
	public static void
	fillRectWithDefaultColor(
		int    minX,
		int    maxX,
		int    minZ,
		int    maxZ,
		byte[] result,
		ColorResolver colorResolver)
	{
		Biome plains = BetterBiomeBlendClient.PLAINS.get();
	
		int color = 0;
		
		if (plains != null)
		{
			color = colorResolver.getColor(plains, 0, 0);
		}
		
		int colorR = Color.RGBAGetR(color);
		int colorG = Color.RGBAGetG(color);
		int colorB = Color.RGBAGetB(color);
		
		for (int z = minZ;
			z < maxZ;
			++z)
		{
			for (int x = minX;
				x < maxX;
				++x)
			{
				result[3 * (16 * z + x) + 0] = (byte)colorR;
				result[3 * (16 * z + x) + 1] = (byte)colorG;
				result[3 * (16 * z + x) + 2] = (byte)colorB;
			}
		}
	}
	
	public static void
	gatherRawColorsToCache(
		World  world,
		int    chunkX,
		int    chunkZ,
		int    blendRadius,
		byte[] result,
		int    chunkIndex,
		ColorResolver colorResolver)
	{
		IChunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);
		
		int minX = getNeighbourRectMinX(chunkIndex, blendRadius);
		int minZ = getNeighbourRectMinZ(chunkIndex, blendRadius);
		int maxX = getNeighbourRectMaxX(chunkIndex, blendRadius);
		int maxZ = getNeighbourRectMaxZ(chunkIndex, blendRadius);
		
		if (chunk != null)
		{
			gatherRawColorsForRect(	
				world,
				chunkX,
				chunkZ,
				minX,
				maxX,
				minZ,
				maxZ,
				result,
				colorResolver);	
		}
		else
		{
			fillRectWithDefaultColor(
				minX,
				maxX,
				minZ,
				maxZ,
				result,
				colorResolver);
		}
	}
	
	public static void
	gatherRawColorsToCaches(
		World           world, 
		int             colorType,
		int             chunkX, 
		int             chunkZ, 
		int             blendRadius, 
		ColorChunkCache rawCache,
		byte[]          result,
		ColorResolver   colorResolver)
	{
		for (int chunkIndex = 0;
			chunkIndex < 9;
			++chunkIndex)
		{
			int offsetX = getNeighbourOffsetX(chunkIndex);
			int offsetZ = getNeighbourOffsetZ(chunkIndex);

			int rawChunkX = chunkX + offsetX;
			int rawChunkZ = chunkZ + offsetZ;
			
			ColorChunk rawChunk = rawCache.getOrDefaultInitializeChunk(rawChunkX, rawChunkZ, colorType);
			
			gatherRawColorsToCache(
				world,
				rawChunkX,
				rawChunkZ,
				blendRadius,
				rawChunk.data,
				chunkIndex,
				colorResolver);
			
			copyRawCacheToBlendCache(rawChunk.data, result, chunkIndex, blendRadius);
		
			rawCache.releaseChunk(rawChunk);
		}
	}
	
	public static void
	copyRawCacheToBlendCache(byte[] rawCache, byte[] result, int chunkIndex, int blendRadius)
	{
		int srcMinX = getNeighbourRectMinX(chunkIndex, blendRadius);
		int srcMinZ = getNeighbourRectMinZ(chunkIndex, blendRadius);
		int srcMaxX = getNeighbourRectMaxX(chunkIndex, blendRadius);
		int srcMaxZ = getNeighbourRectMaxZ(chunkIndex, blendRadius);
		int dstMinX = getNeighbourRectBlendCacheMinX(chunkIndex, blendRadius);
		int dstMinZ = getNeighbourRectBlendCacheMinZ(chunkIndex, blendRadius);
	
		int dstDim = 16 + 2 * blendRadius;
		
		int dstLine = 3 * (dstMinX + dstMinZ * dstDim);
		int srcLine = 3 * (srcMinX + srcMinZ * 16);
		
		for (int z = srcMinZ;
			z < srcMaxZ;
			++z)
		{
			int dstIndex = dstLine;
			int srcIndex = srcLine;
			
			for (int x = srcMinX;
				x < srcMaxX;
				++x)
			{
				result[dstIndex + 0] = rawCache[srcIndex + 0];
				result[dstIndex + 1] = rawCache[srcIndex + 1];
				result[dstIndex + 2] = rawCache[srcIndex + 2];
				
				dstIndex += 3;
				srcIndex += 3;
			}
			
			dstLine += 3 * dstDim;
			srcLine += 3 * 16;
		}
	}
	
	public static void
	blendCachedColorsForChunk(World world, byte[] result, ColorBlendCache blendCache)
	{
		int[] R = blendCache.R;
		int[] G = blendCache.G;
		int[] B = blendCache.B;

		int blendRadius = blendCache.blendRadius;
		int blendDim = 2 * blendRadius + 1;
		int blendCacheDim = 16 + 2 * blendRadius;
		int blendCount = blendDim * blendDim;

		for (int x = 0;
			x < blendCacheDim;
			++x)
		{
			R[x] = 0xFF & blendCache.color[3 * x + 0];
			G[x] = 0xFF & blendCache.color[3 * x + 1];
			B[x] = 0xFF & blendCache.color[3 * x + 2];
		}
		
		for (int z = 1;
			z < blendDim;
			++z)
		{
			for (int x = 0;
				x < blendCacheDim;
				++x)
			{
				R[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 0];
				G[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 1];
				B[x] += 0xFF & blendCache.color[3 * (blendCacheDim * z + x) + 2];
			}
		}
		
		for (int z = 0;
			z < 16;
			++z)
		{
			int accumulatedR = 0;
			int accumulatedG = 0;
			int accumulatedB = 0;
			
			for (int x = 0;
				x < blendDim;
				++x)
			{
				accumulatedR += R[x];
				accumulatedG += G[x];
				accumulatedB += B[x];
			}
			
			for (int x = 0;
				x < 16;
				++x)
			{
				int colorR = accumulatedR / blendCount;
				int colorG = accumulatedG / blendCount;
				int colorB = accumulatedB / blendCount;
				
				result[3 * (16 * z + x) + 0] = (byte)colorR;
				result[3 * (16 * z + x) + 1] = (byte)colorG;
				result[3 * (16 * z + x) + 2] = (byte)colorB;
				
				if (x < 15)
				{
					accumulatedR += R[x + blendDim] - R[x];
					accumulatedG += G[x + blendDim] - G[x];
					accumulatedB += B[x + blendDim] - B[x];
				}
			}
			
			if (z < 15)
			{
				for (int x = 0;
					x < blendCacheDim;
					++x)
				{
					int index1 = 3 * (blendCacheDim * (z           ) + x);
					int index2 = 3 * (blendCacheDim * (z + blendDim) + x);
					
					R[x] += (0xFF & blendCache.color[index2 + 0]) - (0xFF & blendCache.color[index1 + 0]);
					G[x] += (0xFF & blendCache.color[index2 + 1]) - (0xFF & blendCache.color[index1 + 1]);
					B[x] += (0xFF & blendCache.color[index2 + 2]) - (0xFF & blendCache.color[index1 + 2]);
				}
			}
		}
	}

	public static void
	generateBlendedColorChunk(
		World           world,
		int             colorType,
		int             chunkX, 
		int             chunkZ, 
		ColorChunkCache rawCache,
		byte[]          result,
		ColorResolver   colorResolverIn)
	{
		int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();
		
		if (blendRadius >  BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MIN && 
			blendRadius <= BetterBiomeBlendClient.BIOME_BLEND_RADIUS_MAX)
		{
			ColorBlendCache blendCache = acquireBlendCache(blendRadius);
			
			gatherRawColorsToCaches(world, colorType, chunkX, chunkZ, blendCache.blendRadius, rawCache, blendCache.color, colorResolverIn);
			
			blendCachedColorsForChunk(world, result, blendCache);				
			
			releaseBlendCache(blendCache);
		}
		else
		{
			gatherRawColorsForChunk(world, result, chunkX, chunkZ, colorResolverIn);
		}
	}
	
	public static ColorChunk
	getBlendedColorChunk(
		World           world, 
		int             colorType,
		int             chunkX, 
		int             chunkZ, 
		ColorChunkCache cache, 
		ColorChunkCache rawCache,
		ColorResolver   colorResolverIn)
	{
		ColorChunk chunk = cache.getChunk(chunkX, chunkZ, colorType);
		
		if (chunk == null)
		{
			chunk = cache.newChunk(chunkX, chunkZ, colorType);
			
			generateBlendedColorChunk(world, colorType, chunkX, chunkZ, rawCache, chunk.data, colorResolverIn);
			
			cache.putChunk(chunk);
		}
		
		return chunk;
	}
}
