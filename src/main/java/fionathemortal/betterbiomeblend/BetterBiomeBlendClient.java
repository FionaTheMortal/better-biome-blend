package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fionathemortal.betterbiomeblend.mixin.AccessorDoubleOptionSliderWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BetterBiomeBlendClient
{
	public static final Lock lock = new ReentrantLock();
	
	public static final Stack<GenCache> freeGenCaches = new Stack<GenCache>();

	public static final int BIOME_BLEND_RADIUS_MAX = 14;
	public static final int BIOME_BLEND_RADIUS_MIN = 0;

	public static final DoubleOption BIOME_BLEND_RADIUS = new DoubleOption(
		"options.biomeBlendRadius", 
		BIOME_BLEND_RADIUS_MIN,
		BIOME_BLEND_RADIUS_MAX,
		1.0F,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue, 
		BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText);

	public static GameOptions gameOptions;
	
	public static void
	postInitGUIEvent(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight)
	{
		if (screen instanceof VideoOptionsScreen)
		{
			VideoOptionsScreen optionsScreen = (VideoOptionsScreen)screen;
			
			replaceBiomeBlendRadiusOption(optionsScreen);
		}
	}

	@SuppressWarnings("resource")
	public static void
	replaceBiomeBlendRadiusOption(Screen screen)
	{
		List<? extends Element> children = screen.children();
		
		for (Element child : children)
		{
			if (child instanceof ButtonListWidget)
			{
				ButtonListWidget rowList = (ButtonListWidget)child;
				
				List<ButtonListWidget.ButtonEntry> rowListEntries = rowList.children();
				
				boolean replacedOption = false;
				
				for (int index = 0;
					index < rowListEntries.size();
					++index)
				{
					ButtonListWidget.ButtonEntry row = rowListEntries.get(index);
					
					List<? extends Element> rowChildren = row.children();
					
					for (Element rowChild : rowChildren)
					{
						if (rowChild instanceof AccessorDoubleOptionSliderWidget)
						{
							AccessorDoubleOptionSliderWidget accessor = (AccessorDoubleOptionSliderWidget)rowChild;
							
							if (accessor.getOption() == Option.BIOME_BLEND_RADIUS)
							{
								ButtonListWidget.ButtonEntry newRow = ButtonListWidget.ButtonEntry.create(
									MinecraftClient.getInstance().options, 
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
	
	public static Double
	biomeBlendRadiusOptionGetValue(GameOptions settings)
	{
		double result = (double)settings.biomeBlendRadius;
		
		return result;
	}
	
	@SuppressWarnings("resource")
	public static void
	biomeBlendRadiusOptionSetValue(GameOptions settings, Double optionValues)
	{
		int currentValue = (int)optionValues.doubleValue();
		int newSetting   = MathHelper.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);
		
		if (settings.biomeBlendRadius != newSetting)
		{
			settings.biomeBlendRadius = newSetting;
			
			lock.lock();
			
			freeGenCaches.clear();
			
			lock.unlock();
			
			MinecraftClient.getInstance().worldRenderer.reload();
		}
	}
	
	public static Text
	biomeBlendRadiusOptionGetDisplayText(GameOptions settings, DoubleOption optionValues)
	{
		int currentValue  = (int)optionValues.get(settings);
		int blendDiameter = 2 * currentValue + 1;
		
		Text result = new TranslatableText(
			"options.generic_value",
			new TranslatableText("options.biomeBlendRadius"), 
			new TranslatableText("options.biomeBlendRadius." + blendDiameter));
		
		return result;
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

				Option[] enumOptions = (Option[])enumOptionsField.get(null);
				
				boolean found = false;
				
				for (int index = 0;
					index < enumOptions.length;
					++index)
				{
					Option option = enumOptions[index];
					
					if (option == Option.BIOME_BLEND_RADIUS)
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
					// BetterBiomeBlend.LOGGER.warn("Optifine GUI option was not found.");
				}
			}
			catch (Exception e) 
			{
				// BetterBiomeBlend.LOGGER.warn(e);
			}
		} 
		catch (ClassNotFoundException e) 
		{
		}
		
		if (success)
		{
			// BetterBiomeBlend.LOGGER.info("Optifine GUI option was successfully replaced.");
		}
	}

	@SuppressWarnings("resource")
	public static int
	getBlendRadius()
	{
		int result = 0;
		
		if (gameOptions == null)
		{
			gameOptions = MinecraftClient.getInstance().options;
		}
		
		if (gameOptions != null)
		{
			result = gameOptions.biomeBlendRadius;
		}
		
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
			result = new GenCache(getBlendRadius());
		}
		
		return result;
	}
	
	public static void
	releaseGenCache(GenCache cache)
	{
		lock.lock();
		
		if (cache.blendRadius == getBlendRadius())
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
	gatherRawBiomeColorsForChunk(
		BiomeColorType colorType, 
		World          world, 
		int[]          blended,
		int[]          raw, 
		int            chunkX, 
		int            chunkZ, 
		int            offsetX, 
		int            offsetZ, 
		int            blendRadius)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		
		int srcXMin = (offsetX == -1) ? 16 - blendRadius : 0;
		int srcZMin = (offsetZ == -1) ? 16 - blendRadius : 0;
		
		int srcXMax = (offsetX <= 0) ? 16 : blendRadius;
		int srcZMax = (offsetZ <= 0) ? 16 : blendRadius;
		
		int dstX = Math.max(0, offsetX * 16 + blendRadius);
		int dstZ = Math.max(0, offsetZ * 16 + blendRadius);				

		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;

		int resultDim = 16 + 2 * blendRadius;
		
		int dstLine = dstX + dstZ * resultDim;
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
						int color = raw[srcIndex];
						
						if (color == -1)
						{
							blockPos.set(blockX + x2, 0, blockZ + z2);
							
							color = world.getBiome(blockPos).getWaterColor();
							
							raw[srcIndex] = color;
						}
						
						blended[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
					}
					
					dstLine += resultDim;
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
						int color = raw[srcIndex];
						
						if (color == -1)
						{
							blockPos.set(blockX + x2, 0, blockZ + z2);
							
							color = world.getBiome(blockPos).getGrassColorAt(atXF64, atZF64);
							
							raw[srcIndex] = color;
						}
						
						blended[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
						
						atXF64 += 1.0;
					}
					
					dstLine += resultDim;
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
						int color = raw[srcIndex];
						
						if (color == -1)
						{
							blockPos.set(blockX + x2, 0, blockZ + z2);
							
							color = world.getBiome(blockPos).getFoliageColor();
							
							raw[srcIndex] = color;
						}
						
						blended[dstIndex] = color;
						
						++dstIndex;
						++srcIndex;
					}
					
					dstLine += resultDim;
					srcLine += 16;
				}
			} break;
		}
	}
	
	public static void
	gatherRawBiomeColors(World world, int[] result, int chunkX, int chunkZ, int blendRadius, BiomeColorType colorType, ColorChunkCache rawCache)
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
				
				gatherRawBiomeColorsForChunk(colorType, world, result, chunk.data, rawChunkX, rawChunkZ, offsetX, offsetZ, blendRadius);
				
				rawCache.releaseChunk(chunk);
			}
		}
	}
	
	public static void
	generateBlendedColorChunk(World world, int[] result, int chunkX, int chunkZ, BiomeColorType colorType, ColorChunkCache rawCache)
	{
		GenCache cache = acquireGenCache();

		int[] rawColors = cache.colors;
		int blendRadius = cache.blendRadius;

		gatherRawBiomeColors(world, rawColors, chunkX, chunkZ, blendRadius, colorType , rawCache);

		int blendDim    = 2 * blendRadius + 1;
		int blendCount  = blendDim * blendDim;
		int genCacheDim = 16 + 2 * blendRadius;

		int accumulatedR = 0;
		int accumulatedG = 0;
		int accumulatedB = 0;		

		int cacheLine = 0;
		int genCacheIndex = 0;
		
		for (int z = -blendRadius;
			z <= blendRadius;
			++z)
		{
			genCacheIndex = cacheLine;
			
			for (int x = -blendRadius;
				x <= blendRadius;
				++x)
			{
				int color = rawColors[genCacheIndex];
				
				accumulatedR += 255 & (color >> 16);
				accumulatedG += 255 & (color >> 8);
				accumulatedB += 255 &  color;
				
				++genCacheIndex;
			}
			
			cacheLine += genCacheDim;
		}
		
		int colorChunkIndex = 0;
		
		cacheLine = 0;

		for (int z = 0;
			;
			++z)
		{
			genCacheIndex = cacheLine;
			
			int tempR = accumulatedR;
			int tempG = accumulatedG;
			int tempB = accumulatedB;
			
			for (int x = 0;
				;
				++x)
			{
				{
					int colorR = tempR / blendCount;
					int colorG = tempG / blendCount;
					int colorB = tempB / blendCount;
					
					int color = 
						(colorR << 16) |
						(colorG << 8)  |
						(colorB);
					
					result[colorChunkIndex] = color;
				}

				++colorChunkIndex;
				
				if (x < 16 - 1)
				{
					int edgeIndex = genCacheIndex;
					
					for (int endgeZ = -blendRadius;
						endgeZ <= blendRadius;
						++endgeZ)
					{
						int color = rawColors[edgeIndex];
						
						tempR -= 255 & (color >> 16);
						tempG -= 255 & (color >> 8);
						tempB -= 255 &  color;
						
						edgeIndex += genCacheDim;
					}
					
					edgeIndex = genCacheIndex + blendDim;
					
					for (int endgeZ = -blendRadius;
						endgeZ <= blendRadius;
						++endgeZ)
					{
						int color = rawColors[edgeIndex];
						
						tempR += 255 & (color >> 16);
						tempG += 255 & (color >> 8);
						tempB += 255 &  color;
						
						edgeIndex += genCacheDim;
					}
					
					++genCacheIndex;
				}
				else
				{
					break;
				}
			}
			
			if (z < 16 - 1)
			{
				int edgeIndex = cacheLine;
				
				for (int endgeX = -blendRadius;
					endgeX <= blendRadius;
					++endgeX)
				{
					int color = rawColors[edgeIndex];
					
					accumulatedR -= 255 & (color >> 16);
					accumulatedG -= 255 & (color >> 8);
					accumulatedB -= 255 &  color;
					
					edgeIndex += 1;
				}
				
				edgeIndex = cacheLine + blendDim * genCacheDim;
				
				for (int endgeX = -blendRadius;
					endgeX <= blendRadius;
					++endgeX)
				{
					int color = rawColors[edgeIndex];
					
					accumulatedR += 255 & (color >> 16);
					accumulatedG += 255 & (color >> 8);
					accumulatedB += 255 &  color;
					
					edgeIndex += 1;
				}
				
				cacheLine += genCacheDim;
			}
			else
			{
				break;
			}
		}
		
		releaseGenCache(cache);
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