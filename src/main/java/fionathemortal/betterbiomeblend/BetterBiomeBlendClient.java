package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

public class BetterBiomeBlendClient
{
	public static final Lock lock = new ReentrantLock();
	
	public static final Stack<GenCache> freeGenCaches = new Stack<GenCache>();

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

	public static GameSettings gameSettings = Minecraft.getInstance().gameSettings;
	
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
					BetterBiomeBlend.LOGGER.warn("Optifine GUI option was not found.");
				}
			}
			catch (Exception e) 
			{
				BetterBiomeBlend.LOGGER.warn(e);
			}
		} 
		catch (ClassNotFoundException e) 
		{
		}
		
		if (success)
		{
			BetterBiomeBlend.LOGGER.info("Optifine GUI option was successfully replaced.");
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
		int currentValue = (int)optionValues.doubleValue();
		int newSetting   = MathHelper.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);
		
		if (settings.biomeBlendRadius != newSetting)
		{
			settings.biomeBlendRadius = newSetting;
			
			lock.lock();
			
			freeGenCaches.clear();
			
			lock.unlock();
			
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
		
		lock.lock();
		
		if (!freeGenCaches.empty())
		{
			result = freeGenCaches.pop();				
		}
		
		lock.unlock();
		
		if (result == null)
		{
			result = new GenCache(gameSettings.biomeBlendRadius);
		}
		
		return result;
	}
	
	public static void
	releaseGenCache(GenCache cache)
	{
		lock.lock();
		
		if (cache.blendRadius == gameSettings.biomeBlendRadius)
		{
			freeGenCaches.push(cache);
		}
			
		lock.unlock();		
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
	gatherRawColorsForChunk(World world, int[] result, int chunkX, int chunkZ, BiomeColorType colorType)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;

		int dstIndex = 0;
		
		switch(colorType)
		{
			case WATER:
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
						
						result[dstIndex] = world.getBiome(blockPos).getWaterColor();
						
						++dstIndex;
					}
				}
			} break;
			case GRASS:
			{
				double baseXF64 = (double)blockX;
				double baseZF64 = (double)blockZ;
				
				double atZF64 = baseZF64;
				
				for (int z = 0;
					z < 16;
					++z)
				{
					double atXF64 = baseXF64;
					
					for (int x = 0;
						x < 16;
						++x)
					{
						blockPos.setPos(blockX + x, 0, blockZ + z);
						
						result[dstIndex] =  world.getBiome(blockPos).getGrassColor(atXF64, atZF64);
						
						++dstIndex;

						atXF64 += 1.0;
					}
					
					atZF64 += 1.0;
				}
			} break;
			case FOLIAGE:
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
						
						result[dstIndex] = world.getBiome(blockPos).getFoliageColor();
						
						++dstIndex;
					}
				}
			} break;
		}
	}
	
	public static void
	gatherRawColorsForChunkWithCache(
		BiomeColorType colorType, 
		World          world, 
		int[]          result,
		int[]          cache, 
		int            chunkX, 
		int            chunkZ, 
		int            offsetX, 
		int            offsetZ, 
		int            blendRadius)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;
		
		int srcXMin = (offsetX == -1) ? 16 - blendRadius : 0;
		int srcZMin = (offsetZ == -1) ? 16 - blendRadius : 0;
		
		int srcXMax = (offsetX <= 0) ? 16 : blendRadius;
		int srcZMax = (offsetZ <= 0) ? 16 : blendRadius;
		
		int dstX = Math.max(0, offsetX * 16 + blendRadius);
		int dstZ = Math.max(0, offsetZ * 16 + blendRadius);				

		int dstDim = 16 + 2 * blendRadius;
		
		int dstLine = dstX + dstZ * dstDim;
		int srcLine = srcXMin + srcZMin * 16;
		
		switch(colorType)
		{
			case WATER:
			{
				for (int z2 = srcZMin;
					z2 < srcZMax;
					++z2)
				{
					int dstIndex = dstLine;
					int srcIndex = srcLine;
					
					for (int x2 = srcXMin;
						x2 < srcXMax;
						++x2)
					{
						int color = cache[srcIndex];
						
						if (color == -1)
						{
							blockPos.setPos(blockX + x2, 0, blockZ + z2);
							
							color = world.getBiome(blockPos).getWaterColor();

							cache[srcIndex] = color;
						}
						
						result[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
					}
					
					dstLine += dstDim;
					srcLine += 16;
				}
			} break;
			case GRASS:
			{
				double baseXF64 = (double)(blockX + srcXMin);
				double baseZF64 = (double)(blockZ + srcZMin);
				
				double atZF64 = baseZF64;
				
				for (int z2 = srcZMin;
					z2 < srcZMax;
					++z2)
				{
					double atXF64 = baseXF64;
					
					int dstIndex = dstLine;
					int srcIndex = srcLine;
					
					for (int x2 = srcXMin;
						x2 < srcXMax;
						++x2)
					{
						int color = cache[srcIndex];
						
						if (color == -1)
						{
							blockPos.setPos(blockX + x2, 0, blockZ + z2);
														
							color = world.getBiome(blockPos).getGrassColor(atXF64, atZF64);
							
							cache[srcIndex] = color;
						}
						
						result[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
						
						atXF64 += 1.0;
					}
					
					dstLine += dstDim;
					srcLine += 16;
					
					atZF64 += 1.0;
				}
			} break;
			case FOLIAGE:
			{
				for (int z2 = srcZMin;
					z2 < srcZMax;
					++z2)
				{
					int dstIndex = dstLine;
					int srcIndex = srcLine;
					
					for (int x2 = srcXMin;
						x2 < srcXMax;
						++x2)
					{
						int color = cache[srcIndex];
						
						if (color == -1)
						{
							blockPos.setPos(blockX + x2, 0, blockZ + z2);
							
							color = world.getBiome(blockPos).getFoliageColor();
							
							cache[srcIndex] = color;
						}
						
						result[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
					}
					
					dstLine += dstDim;
					srcLine += 16;
				}
			} break;
		}
	}
	
	public static void
	gatherRawColorsToCache(World world, int[] result, int chunkX, int chunkZ, int blendRadius, BiomeColorType colorType, ColorChunkCache rawCache)
	{
		for (int offsetZ = -1;
			offsetZ <= 1;
			++offsetZ)
		{
			for (int offsetX = -1;
				offsetX <= 1;
				++offsetX)
			{
				int rawChunkX = chunkX + offsetX;
				int rawChunkZ = chunkZ + offsetZ;
				
				ColorChunk chunk = rawCache.getChunk(rawChunkX, rawChunkZ);
				
				if (chunk == null)
				{
					chunk = rawCache.newChunk(rawChunkX, rawChunkZ);
					
					Arrays.fill(chunk.data, -1);
					
					rawCache.putChunk(chunk);
				}
				
				gatherRawColorsForChunkWithCache(colorType, world, result, chunk.data, rawChunkX, rawChunkZ, offsetX, offsetZ, blendRadius);
				
				rawCache.releaseChunk(chunk);
			}
		}
	}
	
	// NOTE: Temporary timing code
	
	static AtomicLong accumulatedTime = new AtomicLong();
	static AtomicLong accumulatedCallCount = new AtomicLong();
	
	//
	
	public static void
	blendColorsForChunk(World world, int[] result, GenCache genCache)
	{
		int[] rawColors   = genCache.colors;
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
			int color = rawColors[x];
			
			R[x] = 255 & (color >> 16);
			G[x] = 255 & (color >> 8);
			B[x] = 255 &  color;
		}
		
		for (int z = 1;
			z < blendDim;
			++z)
		{
			for (int x = 0;
				x < genCacheDim;
				++x)
			{
				int color = rawColors[(genCacheDim * z + x)];
				
				R[x] += 255 & (color >> 16);
				G[x] += 255 & (color >> 8);
				B[x] += 255 &  color;
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
				
				int color = 
					(colorR << 16) |
					(colorG << 8)  |
					(colorB);

				result[16 * z + x] = color;
				
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
					int color1 = rawColors[(genCacheDim * (z           ) + x)];
					int color2 = rawColors[(genCacheDim * (z + blendDim) + x)];
					
					R[x] += 255 & ((color2 >> 16) - (color1 >> 16));
					G[x] += 255 & ((color2 >> 8 ) - (color1 >> 8));
					B[x] += 255 &  (color2 -         color1);
				}
			}
		}
	}

	public static void
	generateBlendedColorChunk(World world, int[] result, int chunkX, int chunkZ, BiomeColorType colorType, ColorChunkCache rawCache)
	{
		if (gameSettings.biomeBlendRadius > 0)
		{
			GenCache cache = acquireGenCache();
			
			gatherRawColorsToCache(world, cache.colors, chunkX, chunkZ, cache.blendRadius, colorType , rawCache);
			
			// NOTE: Temporary timing code
			
			long time1 = System.nanoTime();
			
			//
			
			blendColorsForChunk(world, result, cache);
			
			// NOTE: Temporary timing code
			
			long time2 = System.nanoTime();
			long timeD = time2 - time1;
			
			long time = accumulatedTime.addAndGet(timeD);
			long callCount = accumulatedCallCount.addAndGet(1);
			
			if ((callCount & (1024 - 1)) == 0)
			{
				BetterBiomeBlend.LOGGER.info((double)time / (double)callCount);

				accumulatedTime.set(0);
				accumulatedCallCount.set(0);
			}
			
			//
			
			releaseGenCache(cache);
		}
		else
		{
			gatherRawColorsForChunk(world, result, chunkX, chunkZ, colorType);
		}
	}
	
	public static ColorChunk
	getBlendedColorChunk(World world, int chunkX, int chunkZ, ColorChunkCache cache, BiomeColorType colorType, ColorChunkCache rawCache)
	{
		ColorChunk chunk = cache.getChunk(chunkX, chunkZ);
		
		if (chunk == null)
		{
			chunk = cache.newChunk(chunkX, chunkZ);
			
			generateBlendedColorChunk(world, chunk.data, chunkX, chunkZ, colorType, rawCache);
			
			cache.putChunk(chunk);
		}
		
		return chunk;
	}
}