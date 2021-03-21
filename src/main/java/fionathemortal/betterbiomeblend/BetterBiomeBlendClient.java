package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BetterBiomeBlendClient
{
	public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);
	
	public static final Lock            freeGenCacheslock = new ReentrantLock();
	public static final Stack<GenCache> freeGenCaches     = new Stack<GenCache>();

	public static final int BIOME_BLEND_RADIUS_MAX = 14;
	public static final int BIOME_BLEND_RADIUS_MIN = 0;

	public static final SliderPercentageOption BIOME_BLEND_RADIUS = new SliderPercentageOption(
		"options.biomeBlendRadius", 
		BIOME_BLEND_RADIUS_MIN, 
		BIOME_BLEND_RADIUS_MAX, 
		1.0F,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue, 
		BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText);

	public static final GameSettings gameSettings = Minecraft.getInstance().gameSettings;
	
	public static final byte[]
	chunkOffsets = 
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
	
	public static final int[] 
	smallBlendRadiusRegionMasks = 
	{
		(1 << 8),
		(1 << 6) | (1 << 7) | (1 << 8),
		(1 << 6),
		(1 << 2) | (1 << 5) | (1 << 8),	
		(1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8),
		(1 << 0) | (1 << 3) | (1 << 6),
		(1 << 2),
		(1 << 0) | (1 << 1) | (1 << 2),
		(1 << 0)
	};
	
	public static final int[] 
	bigBlendRadiusRegionMasks = 
	{
		(1 << 4) | (1 << 5) | (1 << 7) | (1 << 8),
		(1 << 3) | (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8),
		(1 << 3) | (1 << 4) | (1 << 6) | (1 << 7),
		(1 << 1) | (1 << 2) | (1 << 4) | (1 << 5) | (1 << 7) | (1 << 8),
		(1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8),
		(1 << 0) | (1 << 1) | (1 << 3) | (1 << 4) | (1 << 6) | (1 << 7),
		(1 << 1) | (1 << 2) | (1 << 4) | (1 << 5),			
		(1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5),
		(1 << 0) | (1 << 1) | (1 << 3) | (1 << 4)
	};
	
	public static final byte[]
	regionRectParams = 
	{
		 0,  0,  0,  0, -1,  0, -1,  0,
		-1,  0,  0,  0,  0, -1, -1,  0,
		-1, -1,  0,  0, -1,  0, -1,  0,
		 0,  0, -1,  0, -1,  0,  0, -1,
		-1,  0, -1,  0,  0, -1,  0, -1,
		-1, -1, -1,  0, -1,  0,  0, -1,
		 0,  0, -1, -1, -1,  0, -1,  0,
		-1,  0, -1, -1,  0, -1, -1,  0,
		-1, -1, -1, -1, -1,  0, -1,  0
	};
	
	public static final byte[]
	copyRectParams = 
	{
		1, 1, 0, 0, -1, -1,
		0, 1, 0, 0,  0, -1,
		0, 1, 1, 0,  1, -1,
		1, 0, 0, 0, -1,  0,
		0, 0, 0, 0,  0,  0,
		0, 0, 1, 0,  1,  0,
		1, 0, 0, 1, -1,  1,
		0, 0, 0, 1,  0,  1,
		0, 0, 1, 1,  1,  1		
	};
	
	@SubscribeEvent
	public static void
	postInitGUIEvent(InitGuiEvent.Post event)
	{
		Screen screen = event.getGui();
		
		if (screen instanceof VideoSettingsScreen)
		{
			VideoSettingsScreen videoSettingsScreen = (VideoSettingsScreen)screen;
			
			replaceBiomeBlendRadiusOption(videoSettingsScreen);
		}
	}

	@SuppressWarnings("resource")
	public static void
	replaceBiomeBlendRadiusOption(VideoSettingsScreen screen)
	{
		List<? extends IGuiEventListener> children = screen.getEventListeners();
		
		for (IGuiEventListener child : children)
		{
			if (child instanceof OptionsRowList)
			{
				OptionsRowList rowList = (OptionsRowList)child;
				
				List<OptionsRowList.Row> rowListEntries = rowList.getEventListeners();
				
				boolean replacedOption = false;
				
				for (int index = 0;
					index < rowListEntries.size();
					++index)
				{
					OptionsRowList.Row row = rowListEntries.get(index);
					
					List<? extends IGuiEventListener> rowChildren = row.getEventListeners();
					
					for (IGuiEventListener rowChild : rowChildren)
					{
						if (rowChild instanceof AccessorOptionSlider)
						{
							AccessorOptionSlider accessor = (AccessorOptionSlider)rowChild;
							
							if (accessor.getOption() == AbstractOption.BIOME_BLEND_RADIUS)
							{
								OptionsRowList.Row newRow = OptionsRowList.Row.create(
									screen.getMinecraft().gameSettings, 
									screen.width, 
									BIOME_BLEND_RADIUS);
								
								rowListEntries.set(index, newRow);
								
								replacedOption = true;
							}
						}
					}
					
					if (replacedOption)
					{
						break;
					}
				}
			}
		}
	}
    
	public static void
	overwriteOptifineGUIBlendRadiusOption()
	{
		boolean success = false;

		try
		{
			Class<?> guiDetailSettingsOFClass = Class.forName("net.optifine.gui.GuiDetailSettingsOF");
			
			try 
			{
				Field enumOptionsField = guiDetailSettingsOFClass.getDeclaredField("enumOptions");
				
				enumOptionsField.setAccessible(true);
			
				AbstractOption[] enumOptions = (AbstractOption[])enumOptionsField.get(null);
				
				boolean found = false;
				
				for (int index = 0;
					index < enumOptions.length;
					++index)
				{
					AbstractOption option = enumOptions[index];
					
					if (option == AbstractOption.BIOME_BLEND_RADIUS)
					{
						enumOptions[index] = BIOME_BLEND_RADIUS;
						
						found = true;
						
						break;
					}
				}
				
				if (found)
				{
					success = true;
				}
				else
				{
					LOGGER.warn("Optifine GUI option was not found.");
				}
			}
			catch (Exception e) 
			{
				LOGGER.warn(e);
			}
		} 
		catch (ClassNotFoundException e) 
		{
		}
		
		if (success)
		{
			LOGGER.info("Optifine GUI option was successfully replaced.");
		}
	}
	
	public static Double
	biomeBlendRadiusOptionGetValue(GameSettings settings)
	{
		double result = (double)settings.biomeBlendRadius;
		
		return result;
	}
	
	@SuppressWarnings("resource")
	public static void
	biomeBlendRadiusOptionSetValue(GameSettings settings, Double optionValues)
	{
		/* BUG: Concurrent modification exception with structure generation
		 * But this code is a 1 to 1 copy of vanilla code so it might just be an unlikely bug on their end */
		
		int currentValue = (int)optionValues.doubleValue();
		int newSetting   = MathHelper.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);
		
		if (settings.biomeBlendRadius != newSetting)
		{
			settings.biomeBlendRadius = newSetting;
			
			freeGenCacheslock.lock();
			
			freeGenCaches.clear();
			
			freeGenCacheslock.unlock();
			
			Minecraft.getInstance().worldRenderer.loadRenderers();
		}
	}
	
	public static ITextComponent
	biomeBlendRadiusOptionGetDisplayText(GameSettings settings, SliderPercentageOption optionValues)
	{
		int currentValue  = (int)optionValues.get(settings);
		int blendDiameter = 2 * currentValue + 1;
		
		ITextComponent result = new TranslationTextComponent(
			"options.generic_value",
			new TranslationTextComponent("options.biomeBlendRadius"), 
			new TranslationTextComponent("options.biomeBlendRadius." + blendDiameter));
		
		return result;
	}

	public static GenCache
	acquireGenCache()
	{
		GenCache result = null;
		
		freeGenCacheslock.lock();
		
		if (!freeGenCaches.empty())
		{
			result = freeGenCaches.pop();				
		}
		
		freeGenCacheslock.unlock();
		
		if (result == null)
		{
			result = new GenCache(gameSettings.biomeBlendRadius);
		}
		
		return result;
	}
	
	public static void
	releaseGenCache(GenCache cache)
	{
		freeGenCacheslock.lock();
		
		if (cache.blendRadius == gameSettings.biomeBlendRadius)
		{
			freeGenCaches.push(cache);
		}
			
		freeGenCacheslock.unlock();		
	}
		
	public static ColorChunk
	getThreadLocalChunk(ThreadLocal<ColorChunk> threadLocal, int chunkX, int chunkZ)
	{
		ColorChunk result = null;
		ColorChunk local = threadLocal.get();
		
		long key = ColorChunkCache.getChunkKey(chunkX, chunkZ);
		
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
	gatherRawColorsForChunk(World world, byte[] result, int chunkX, int chunkZ, int colorType)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;

		int dstIndex = 0;
		
		switch(colorType)
		{
			case BiomeColorType.GRASS:
			{
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
						
						int color =  world.getBiome(blockPos).getGrassColor(xF64, zF64);
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * dstIndex + 0] = (byte)colorR;
						result[3 * dstIndex + 1] = (byte)colorG;
						result[3 * dstIndex + 2] = (byte)colorB;
	
						++dstIndex;
	
						xF64 += 1.0;
					}
					
					zF64 += 1.0;
				}
			} break;
			case BiomeColorType.WATER:
			{
				for (int z = 0;
					z < 16;
					++z)
				{
					for (int x = 0;
						x < 16;
						++x)
					{
						blockPos.setPos(blockX + x, 0, blockZ + z);
						
						int color = world.getBiome(blockPos).getWaterColor();
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * dstIndex + 0] = (byte)colorR;
						result[3 * dstIndex + 1] = (byte)colorG;
						result[3 * dstIndex + 2] = (byte)colorB;

						++dstIndex;
					}
				}
			} break;
			case BiomeColorType.FOLIAGE:
			{
				for (int z = 0;
					z < 16;
					++z)
				{
					for (int x = 0;
						x < 16;
						++x)
					{
						blockPos.setPos(blockX + x, 0, blockZ + z);
						
						int color = world.getBiome(blockPos).getFoliageColor();
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * dstIndex + 0] = (byte)colorR;
						result[3 * dstIndex + 1] = (byte)colorG;
						result[3 * dstIndex + 2] = (byte)colorB;

						++dstIndex;
					}
				}
			} break;
		}
	}
	
	public static void
	gatherRawColorsForArea(
		World          world,
		int            colorType,
		int            chunkX,
		int            chunkZ,
		int            minX,
		int            maxX,
		int            minZ,
		int            maxZ,
		byte[]         result)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = 16 * chunkX;
		int blockZ = 16 * chunkZ;
		
		switch(colorType)
		{
			case BiomeColorType.GRASS:
			{
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
						blockPos.setPos(blockX + x, 0, blockZ + z);
							
						int color = world.getBiome(blockPos).getGrassColor(xF64, zF64);
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * (16 * z + x) + 0] = (byte)colorR;
						result[3 * (16 * z + x) + 1] = (byte)colorG;
						result[3 * (16 * z + x) + 2] = (byte)colorB;

						xF64 += 1.0;
					}
					
					zF64 += 1.0;
				}
			} break;
			case BiomeColorType.WATER:
			{
				for (int z = minZ;
					z < maxZ;
					++z)
				{
					for (int x = minX;
						x < maxX;
						++x)
					{
						blockPos.setPos(blockX + x, 0, blockZ + z);
						
						int color = world.getBiome(blockPos).getWaterColor();
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * (16 * z + x) + 0] = (byte)colorR;
						result[3 * (16 * z + x) + 1] = (byte)colorG;
						result[3 * (16 * z + x) + 2] = (byte)colorB;
					}
				}
			} break;
			case BiomeColorType.FOLIAGE:
			{
				for (int z = minZ;
					z < maxZ;
					++z)
				{
					for (int x = minX;
						x < maxX;
						++x)
					{
						blockPos.setPos(blockX + x, 0, blockZ + z);
						
						int color = world.getBiome(blockPos).getFoliageColor();
						
						int colorR = 0xFF &  color;
						int colorG = 0xFF & (color >>  8);
						int colorB = 0xFF & (color >> 16);
						
						result[3 * (16 * z + x) + 0] = (byte)colorR;
						result[3 * (16 * z + x) + 1] = (byte)colorG;
						result[3 * (16 * z + x) + 2] = (byte)colorB;
					}
				}
			} break;
		}
	}
	
	public static void
	gatherRawColorsForRegions(
		World  world,
		int    colorType,
		int    chunkX,
		int    chunkZ,
		int    regions,
		int    blendRadius,
		byte[] result)
	{
		int cornerRegionD;

		if (blendRadius <= 16 / 2)
		{
			cornerRegionD = blendRadius;
		}
		else
		{
			cornerRegionD = 16 - blendRadius; 
		}
		
		int centerRegionW = 16 - 2 * cornerRegionD;
		
		for (int index = 0;
			index < 9;
			++index)
		{
			if ((regions & (1 << index)) != 0)
			{
				int arrayOffset = 8 * index;
				
				int minX = 
					(cornerRegionD & regionRectParams[arrayOffset + 0]) + 
					(centerRegionW & regionRectParams[arrayOffset + 1]);
				
				int minZ = 
					(cornerRegionD & regionRectParams[arrayOffset + 2]) + 
					(centerRegionW & regionRectParams[arrayOffset + 3]);
				
				int dimX = 
					(cornerRegionD & regionRectParams[arrayOffset + 4]) + 
					(centerRegionW & regionRectParams[arrayOffset + 5]);
				
				int dimZ = 
					(cornerRegionD & regionRectParams[arrayOffset + 6]) + 
					(centerRegionW & regionRectParams[arrayOffset + 7]);
				
				int maxX = minX + dimX;
				int maxZ = minZ + dimZ;
				
				gatherRawColorsForArea(	
					world,
					colorType,
					chunkX,
					chunkZ,
					minX,
					maxX,
					minZ,
					maxZ,
					result);
			}
		}
	}
	
	public static AtomicLong missRate = new AtomicLong();
	public static AtomicLong hitRate = new AtomicLong();
	
	public static void
	gatherRawColorsToCaches(
		World           world, 
		byte[]          result2, 
		int             chunkX, 
		int             chunkZ, 
		int             blendRadius, 
		int             colorType, 
		ColorChunkCache rawCache)
	{
		int[] regionMasks = blendRadius > 8 ? bigBlendRadiusRegionMasks : smallBlendRadiusRegionMasks;
		
		for (int index = 0;
			index < 9;
			++index)
		{
			int offsetX = chunkOffsets[2 * index + 0];
			int offsetZ = chunkOffsets[2 * index + 1];

			int rawChunkX = chunkX + offsetX;
			int rawChunkZ = chunkZ + offsetZ;
			
			ColorChunk chunk = rawCache.getChunk(rawChunkX, rawChunkZ);
			
			if (chunk == null)
			{
				chunk = rawCache.newChunk(rawChunkX, rawChunkZ);
				
				chunk.regionMask.set(0);
				
				rawCache.putChunk(chunk);
			}
			
			int requiredRegions = regionMasks[index];

			int currentRegions = chunk.regionMask.get();
			int missingRegions = requiredRegions & ~currentRegions; 
			
			if (missingRegions != 0)
			{
				gatherRawColorsForRegions(
					world, 
					colorType, 
					rawChunkX, 
					rawChunkZ, 
					missingRegions, 
					blendRadius, 
					chunk.data);
				
				currentRegions = chunk.regionMask.get();
				
				chunk.regionMask.set(currentRegions | requiredRegions);
			}
			
			copyRawCacheToGenCache(chunk.data, result2, index, blendRadius);
		
			rawCache.releaseChunk(chunk);
		}
	}
	
	public static void
	copyRawCacheToGenCache(
		byte[] rawCache,
		byte[] result,
		int    chunkIndex,
		int    blendRadius)
	{
		int LUTIndex = 6 * chunkIndex;
		
		int srcMinX = copyRectParams[LUTIndex + 0] * (16 - blendRadius);
		int srcMinZ = copyRectParams[LUTIndex + 1] * (16 - blendRadius);
		
		int srcMaxX = copyRectParams[LUTIndex + 2] * (blendRadius - 16) + 16;
		int srcMaxZ = copyRectParams[LUTIndex + 3] * (blendRadius - 16) + 16;
		
		int dstMinX = Math.max((copyRectParams[LUTIndex + 4] << 4) + blendRadius, 0);
		int dstMinZ = Math.max((copyRectParams[LUTIndex + 5] << 4) + blendRadius, 0);
	
		int dstDim = 16 + 2 * blendRadius;
		
		int dstLine = dstMinX + dstMinZ * dstDim;
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
				result[3 * dstIndex + 0] = rawCache[srcIndex + 0];
				result[3 * dstIndex + 1] = rawCache[srcIndex + 1];
				result[3 * dstIndex + 2] = rawCache[srcIndex + 2];
				
				dstIndex++;
				srcIndex += 3;
			}
			
			dstLine += dstDim;
			srcLine += 3 * 16;
		}
	}
	
	// NOTE: Temporary timing code
	
	static AtomicLong accumulatedTime = new AtomicLong();
	static AtomicLong accumulatedCallCount = new AtomicLong();
	
	//
	
	public static void
	blendCachedColorsForChunk(World world, byte[] result, GenCache genCache)
	{
		int   blendRadius = genCache.blendRadius;

		int[] R = genCache.R;
		int[] G = genCache.G;
		int[] B = genCache.B;
		
		int blendDim    = 2 * blendRadius + 1;
		int genCacheDim = 16 + 2 * blendRadius;
		int blendCount  = blendDim * blendDim;

		for (int x = 0;
			x < genCacheDim;
			++x)
		{
			R[x] = 0xFF & genCache.color[3 * x + 0];
			G[x] = 0xFF & genCache.color[3 * x + 1];
			B[x] = 0xFF & genCache.color[3 * x + 2];
		}
		
		for (int z = 1;
			z < blendDim;
			++z)
		{
			for (int x = 0;
				x < genCacheDim;
				++x)
			{
				R[x] += 0xFF & genCache.color[3 * (genCacheDim * z + x) + 0];
				G[x] += 0xFF & genCache.color[3 * (genCacheDim * z + x) + 1];
				B[x] += 0xFF & genCache.color[3 * (genCacheDim * z + x) + 2];
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
					x < genCacheDim;
					++x)
				{
					int index1 = 3 * (genCacheDim * (z           ) + x);
					int index2 = 3 * (genCacheDim * (z + blendDim) + x);
					
					R[x] += (0xFF & genCache.color[index2 + 0]) - (0xFF & genCache.color[index1 + 0]);
					G[x] += (0xFF & genCache.color[index2 + 1]) - (0xFF & genCache.color[index1 + 1]);
					B[x] += (0xFF & genCache.color[index2 + 2]) - (0xFF & genCache.color[index1 + 2]);
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
		byte[]          result)
	{
		if (gameSettings.biomeBlendRadius > 0 && 
			gameSettings.biomeBlendRadius <= BIOME_BLEND_RADIUS_MAX)
		{
			GenCache cache = acquireGenCache();
			
			gatherRawColorsToCaches(world, cache.color, chunkX, chunkZ, cache.blendRadius, colorType , rawCache);
			
			blendCachedColorsForChunk(world, result, cache);
			
			releaseGenCache(cache);
		}
		else
		{
			gatherRawColorsForChunk(world, result, chunkX, chunkZ, colorType);
		}
	}
	
	public static ColorChunk
	getBlendedColorChunk(
		World           world, 
		int             colorType,
		int             chunkX, 
		int             chunkZ, 
		ColorChunkCache cache, 
		ColorChunkCache rawCache)
	{
		ColorChunk chunk = cache.getChunk(chunkX, chunkZ);
		
		if (chunk == null)
		{
			chunk = cache.newChunk(chunkX, chunkZ);
			
			// NOTE: Temporary timing code
			
			long time1 = System.nanoTime();
			
			//
			
			generateBlendedColorChunk(world, colorType, chunkX, chunkZ, rawCache, chunk.data);
			
			// NOTE: Temporary timing code
			
			long time2 = System.nanoTime();
			long timeD = time2 - time1;
			
			long time = accumulatedTime.addAndGet(timeD);
			long callCount = accumulatedCallCount.addAndGet(1);
			
			if ((callCount & (1024 - 1)) == 0)
			{
				LOGGER.info((double)time / (double)callCount);

				accumulatedTime.set(0);
				accumulatedCallCount.set(0);
			}
			
			//
			
			cache.putChunk(chunk);
		}
		
		return chunk;
	}
}