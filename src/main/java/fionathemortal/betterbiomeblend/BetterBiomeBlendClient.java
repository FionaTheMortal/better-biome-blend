package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Stack;

import fionathemortal.betterbiomeblend.mixin.AccessorChunkRenderCache;
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
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BetterBiomeBlendClient
{
	public static final Stack<GenCache> freeGenCaches = new Stack<GenCache>();

	public static final ThreadLocal<ColorChunk> threadLocalWaterChunk   = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<ColorChunk> threadLocalGrassChunk   = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<ColorChunk> threadLocalFoliageChunk = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	
	public static final ColorChunkCache waterColorCache   = new ColorChunkCache(1024);
	public static final ColorChunkCache grassColorCache   = new ColorChunkCache(1024);
	public static final ColorChunkCache foliageColorCache = new ColorChunkCache(1024);
	
	public static final ColorChunkCache rawWaterColorCache   = new ColorChunkCache(256);
	public static final ColorChunkCache rawGrassColorCache   = new ColorChunkCache(256);
	public static final ColorChunkCache rawFoliageColorCache = new ColorChunkCache(256);
	
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
			
			synchronized (freeGenCaches)
			{
				freeGenCaches.clear();
			}
			
			waterColorCache.invalidateAll();
			grassColorCache.invalidateAll();
			foliageColorCache.invalidateAll();
			
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
	

	@SubscribeEvent
	public static void
	chunkLoadedEvent(ChunkEvent.Load event)
	{
		chunkWasLoaded(event.getChunk());
	}
    
	public static void
	chunkWasLoaded(IChunk chunk)
	{
		int chunkX = chunk.getPos().x;
		int chunkZ = chunk.getPos().z;
		
		waterColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		grassColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		foliageColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		
		rawWaterColorCache.invalidateChunk(chunkX, chunkZ);
	}
	
	public static GenCache
	acquireGenCache()
	{
		GenCache result = null;
		
		synchronized(freeGenCaches)
		{
			if (!freeGenCaches.empty())
			{
				result = freeGenCaches.pop();				
			}
		}
		
		if (result == null)
		{
			result = new GenCache(gameSettings.biomeBlendRadius);
		}
		
		return result;
	}
	
	public static void
	releaseGenCache(GenCache cache)
	{
		synchronized(freeGenCaches)
		{
			if (cache.blendRadius == gameSettings.biomeBlendRadius)
			{
				freeGenCaches.push(cache);
			}
		}
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
	gatherRawBiomeColorsForChunk(BiomeColorType colorType, IWorldReader world, int[] result, int chunkX, int chunkZ)
	{
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		
		int genCacheIndex = 0;
		
		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;
		
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
						int posX = blockX + x;
						int posZ = blockZ + z;
						
						blockPos.setPos(posX, 0, posZ);
						
						result[genCacheIndex] = world.getBiome(blockPos).getWaterColor();
						
						++genCacheIndex;
					}
				}
			} break;
			case GRASS:
			{
				double baseXF64 = (double)blockX;
				double baseZF64 = (double)blockZ;
				
				double atZF64 = baseZF64;
				
				for (int indexZ = 0;
					indexZ < 16;
					++indexZ)
				{
					double atXF64 = baseXF64;
					
					for (int indexX = 0;
						indexX < 16;
						++indexX)
					{
						int posX = blockX + indexX;
						int posZ = blockZ + indexZ;
						
						blockPos.setPos(posX, 0, posZ);
						
						result[genCacheIndex] = world.getBiome(blockPos).getGrassColor(atXF64, atZF64);
						
						++genCacheIndex;
						
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
						int posX = blockX + x;
						int posZ = blockZ + z;
						
						blockPos.setPos(posX, 0, posZ);
						
						result[genCacheIndex] = world.getBiome(blockPos).getFoliageColor();
						
						++genCacheIndex;
					}
				}
			} break;
		}
	}
	
	public static void
	gatherRawBiomeColors(IWorldReader world, int[] result, int chunkX, int chunkZ, int blendRadius, BiomeColorType colorType, ColorChunkCache rawCache)
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
					
					gatherRawBiomeColorsForChunk(colorType, world, chunk.data, rawChunkX, rawChunkZ);
					
					rawCache.putChunk(chunk);
				}
				
				int srcXMin = (offsetX == -1) ? 16 - blendRadius : 0;
				int srcZMin = (offsetZ == -1) ? 16 - blendRadius : 0;
				
				int srcXMax = (offsetX <= 0) ? 16 : blendRadius;
				int srcZMax = (offsetZ <= 0) ? 16 : blendRadius;
				
				int dstX = Math.max(0, offsetX * 16 + blendRadius);
				int dstZ = Math.max(0, offsetZ * 16 + blendRadius);
				
				int resultDim = 16 + 2 * blendRadius;
				
				int dstLine = dstX + dstZ * resultDim;
				int srcLine = srcXMin + srcZMin * 16;
				
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
						result[dstIndex] = chunk.data[srcIndex];
						
						++dstIndex;
						++srcIndex;
					}
					
					dstLine += resultDim;
					srcLine += 16;
				}
				
				rawCache.releaseChunk(chunk);
			}
		}
	}
	
	public static void
	generateBlendedColorChunk(IWorldReader world, int[] result, int chunkX, int chunkZ, BiomeColorType colorType, ColorChunkCache rawCache)
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
	
	public static IWorldReader
	getIWorldReader(IBlockDisplayReader blockDisplayReader)
	{
		IWorldReader result = null;
		
		if (blockDisplayReader instanceof AccessorChunkRenderCache)
		{
			AccessorChunkRenderCache accessor = (AccessorChunkRenderCache)blockDisplayReader; 
			
			result = accessor.getWorld();
		}
		else if (blockDisplayReader instanceof IWorldReader)
		{
			result = (IWorldReader)blockDisplayReader;
		}
		
		return result;
	}
	
	public static ColorChunk
	getBlendedColorChunk(IBlockDisplayReader worldIn, int chunkX, int chunkZ, ColorChunkCache cache, BiomeColorType colorType, ColorChunkCache rawCache)
	{
		ColorChunk chunk = cache.getChunk(chunkX, chunkZ);
		
		if (chunk == null)
		{
			IWorldReader world = getIWorldReader(worldIn);
			
			if (world != null)
			{
				chunk = cache.newChunk(chunkX, chunkZ);
				
				generateBlendedColorChunk(world, chunk.data, chunkX, chunkZ, colorType, rawCache);
				
				cache.putChunk(chunk);	
			}
		}
		
		return chunk;
	}

	public static int
	getWaterColor(IBlockDisplayReader world, BlockPos blockPos)
	{
		int result;
		
		int x = blockPos.getX();
		int z = blockPos.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = getThreadLocalChunk(threadLocalWaterChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.
			
			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, waterColorCache, BiomeColorType.WATER, rawWaterColorCache);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalWaterChunk, chunk, waterColorCache);				

				result = chunk.getColor(x, z);
			}
			else
			{
				// NOTE: Call the vanilla code path for compatibility
				
				result = world.getBlockColor(blockPos, BiomeColors.WATER_COLOR);
			}
		}
	
		return result;
	}
	
	public static int
	getGrassColor(IBlockDisplayReader world, BlockPos blockPos)
	{
		int result;
		
		int x = blockPos.getX();
		int z = blockPos.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = getThreadLocalChunk(threadLocalGrassChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.

			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, grassColorCache, BiomeColorType.GRASS, rawGrassColorCache);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalGrassChunk, chunk, grassColorCache);
				
				result = chunk.getColor(x, z);
			}
			else
			{
				// NOTE: Call the vanilla code path for compatibility
				
				result = world.getBlockColor(blockPos, BiomeColors.GRASS_COLOR);
			}
		}
	
		return result;
	}
	
	public static int
	getFoliageColor(IBlockDisplayReader world, BlockPos blockPos)
	{
		int result;
		
		int x = blockPos.getX();
		int z = blockPos.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = getThreadLocalChunk(threadLocalFoliageChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.

			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, foliageColorCache, BiomeColorType.FOLIAGE, rawFoliageColorCache);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalFoliageChunk, chunk, foliageColorCache);				

				result = chunk.getColor(x, z);
			}
			else
			{
				// NOTE: Call the vanilla code path for compatibility
				
				result = world.getBlockColor(blockPos, BiomeColors.FOLIAGE_COLOR);
			}
		}
	
		return result;
	}
}