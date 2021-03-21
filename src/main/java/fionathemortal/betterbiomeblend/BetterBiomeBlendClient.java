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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

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
		
		while (!freeGenCaches.empty())
		{
			result = freeGenCaches.pop();
			
			if (result.blendRadius == gameSettings.biomeBlendRadius)
			{
				break;
			}
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
						
						int colorR = Color.RGBAGetR(color);
						int colorG = Color.RGBAGetG(color);
						int colorB = Color.RGBAGetB(color);
						
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
						
						int colorR = Color.RGBAGetR(color);
						int colorG = Color.RGBAGetG(color);
						int colorB = Color.RGBAGetB(color);
						
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
		World  world,
		int    colorType,
		int    chunkX,
		int    chunkZ,
		int    minX,
		int    maxX,
		int    minZ,
		int    maxZ,
		byte[] result)
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
						
						int colorR = Color.RGBAGetR(color);
						int colorG = Color.RGBAGetG(color);
						int colorB = Color.RGBAGetB(color);
						
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
						
						int colorR = Color.RGBAGetR(color);
						int colorG = Color.RGBAGetG(color);
						int colorB = Color.RGBAGetB(color);
						
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
						
						int colorR = Color.RGBAGetR(color);
						int colorG = Color.RGBAGetG(color);
						int colorB = Color.RGBAGetB(color);
						
						result[3 * (16 * z + x) + 0] = (byte)colorR;
						result[3 * (16 * z + x) + 1] = (byte)colorG;
						result[3 * (16 * z + x) + 2] = (byte)colorB;
					}
				}
			} break;
		}
	}
	
	public static final RegistryObject<Biome> PLAINS = RegistryObject.of(
		new ResourceLocation("minecraft:plains"), ForgeRegistries.BIOMES);

	public static void
	fillWithDefaultColor(		
		World  world,
		int    colorType,
		int    chunkX,
		int    chunkZ,
		int    minX,
		int    maxX,
		int    minZ,
		int    maxZ,
		byte[] result)
	{
		int color = 0;
		
		Biome plains = PLAINS.get();
		
		if (plains != null)
		{
			switch(colorType)
			{
				case BiomeColorType.GRASS:
				{
					color = plains.getGrassColor(0, 0);
				} break;
				case BiomeColorType.WATER:
				{
					color = plains.getWaterColor();
				} break;
				case BiomeColorType.FOLIAGE:
				{
					color = plains.getFoliageColor();
				} break;
			}
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
		
		IChunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);
		
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
				
				if (chunk != null)
				{
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
				else
				{
					fillWithDefaultColor(
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
	}
	
	public static void
	gatherRawColorsToCaches(
		World           world, 
		int             colorType,
		int             chunkX, 
		int             chunkZ, 
		int             blendRadius, 
		ColorChunkCache rawCache,
		byte[]          result)
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
			
			ColorChunk rawChunk = rawCache.getChunk(rawChunkX, rawChunkZ, colorType);
			
			if (rawChunk == null)
			{
				rawChunk = rawCache.newChunk(rawChunkX, rawChunkZ, colorType);
				
				rawChunk.regionMask.set(0);
				
				rawCache.putChunk(rawChunk);
			}
			
			int requiredRegions = regionMasks[index];

			int currentRegions = rawChunk.getValidRegions();
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
					rawChunk.data);
				
				rawChunk.addToValidRegions(requiredRegions);
			}
			
			copyRawCacheToGenCache(rawChunk.data, result, index, blendRadius);
		
			rawCache.releaseChunk(rawChunk);
		}
	}
	
	public static void
	copyRawCacheToGenCache(byte[] rawCache, byte[] result, int chunkIndex, int blendRadius)
	{
		int rectParamsOffset = 6 * chunkIndex;
		
		int srcMinX = copyRectParams[rectParamsOffset + 0] * (16 - blendRadius);
		int srcMinZ = copyRectParams[rectParamsOffset + 1] * (16 - blendRadius);
		
		int srcMaxX = copyRectParams[rectParamsOffset + 2] * (blendRadius - 16) + 16;
		int srcMaxZ = copyRectParams[rectParamsOffset + 3] * (blendRadius - 16) + 16;
		
		int dstMinX = Math.max((copyRectParams[rectParamsOffset + 4] << 4) + blendRadius, 0);
		int dstMinZ = Math.max((copyRectParams[rectParamsOffset + 5] << 4) + blendRadius, 0);
	
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
			
			gatherRawColorsToCaches(world, colorType, chunkX, chunkZ, cache.blendRadius, rawCache, cache.color);
			
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
		ColorChunk chunk = cache.getChunk(chunkX, chunkZ, colorType);
		
		if (chunk == null)
		{
			chunk = cache.newChunk(chunkX, chunkZ, colorType);
			
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