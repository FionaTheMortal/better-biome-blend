package fionathemortal.betterbiomeblend;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fionathemortal.betterbiomeblend.mixin.AccessorChunkRenderCache;
import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;

import java.util.List;
import java.util.Stack;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlend 
{
	public static final String MOD_ID = "betterbiomeblend";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	public static final int chunkDim = 16;
	
	public static final Stack<GenCache> freeGenCaches = new Stack<GenCache>();
	
	public static final ThreadLocal<BlendedColorChunk> threadLocalWaterChunk   = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<BlendedColorChunk> threadLocalGrassChunk   = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<BlendedColorChunk> threadLocalFoliageChunk = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	
	public static final BlendedColorCache waterColorCache   = new BlendedColorCache(512);
	public static final BlendedColorCache grassColorCache   = new BlendedColorCache(512);
	public static final BlendedColorCache foliageColorCache = new BlendedColorCache(512);

	public static final int BIOME_BLEND_RADIUS_MAX = 14;
	public static final int BIOME_BLEND_RADIUS_MIN = 0;
	
	public static final SliderPercentageOption BIOME_BLEND_RADIUS = new SliderPercentageOption(
		"options.biomeBlendRadius", 
		BIOME_BLEND_RADIUS_MIN, 
		BIOME_BLEND_RADIUS_MAX, 
		1.0F,
		BetterBiomeBlend::biomeBlendRadiusOptionGetValue, 
		BetterBiomeBlend::biomeBlendRadiusOptionSetValue,
		BetterBiomeBlend::biomeBlendRadiusOptionGetDisplayText
	);

	public static GameSettings gameSettings = Minecraft.getInstance().gameSettings;
	
	public
	BetterBiomeBlend()
	{
        ModLoadingContext.get().registerExtensionPoint(
    		ExtensionPoint.DISPLAYTEST, 
    		() -> Pair.of(
				() -> "client-only", 
				(v, n) -> n));
		
		MinecraftForge.EVENT_BUS.register(BetterBiomeBlend.class);
	}

	@SubscribeEvent
	public static void
	chunkLoadedEvent(ChunkEvent.Load event)
	{
		chunkWasLoaded(event.getChunk());
	}
	
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
    
	public static void
	chunkWasLoaded(IChunk chunk)
	{
		int chunkX = chunk.getPos().x;
		int chunkZ = chunk.getPos().z;
		
		waterColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		grassColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		foliageColorCache.invalidateNeighbourhood(chunkX, chunkZ);
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
		
	public static BlendedColorChunk
	getThreadLocalChunk(ThreadLocal<BlendedColorChunk> threadLocal, int chunkX, int chunkZ)
	{
		BlendedColorChunk result = null;
		BlendedColorChunk local = threadLocal.get();
		
		long key = BlendedColorCache.getChunkKey(chunkX, chunkZ);
		
		if (local.key == key && local.refCount.get() > 1)
		{
			result = local;
		}
		
		return result;
	}
	
	public static void
	setThreadLocalChunk(ThreadLocal<BlendedColorChunk> threadLocal, BlendedColorChunk chunk, BlendedColorCache cache)
	{
		BlendedColorChunk local = threadLocal.get();
		
		cache.releaseChunk(local);
		
		threadLocal.set(chunk);
	}
	
	public static void
	gatherBiomeColors(BiomeColorType colorType, IWorldReader world, int[] result, int blockX, int blockZ, int blendRadius)
	{
		switch(colorType)
		{
			case WATER:
			{
				BlockPos.Mutable blockPos = new BlockPos.Mutable();
				
				int genCacheIndex = 0;
				
				for (int z = -blendRadius;
					z < BetterBiomeBlend.chunkDim + blendRadius;
					++z)
				{
					for (int x = -blendRadius;
						x < BetterBiomeBlend.chunkDim + blendRadius;
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
				BlockPos.Mutable blockPos = new BlockPos.Mutable();
				
				int genCacheIndex = 0;
				
				int baseX = blockX - blendRadius;
				int baseZ = blockZ - blendRadius;
				
				double baseXF64 = (double)baseX;
				double baseZF64 = (double)baseZ;
				
				double atZF64 = baseZF64;
				
				for (int indexZ = 0;
					indexZ < BetterBiomeBlend.chunkDim + 2 * blendRadius;
					++indexZ)
				{
					double atXF64 = baseXF64;
					
					for (int indexX = 0;
						indexX < BetterBiomeBlend.chunkDim + 2 * blendRadius;
						++indexX)
					{
						int posX = baseX + indexX;
						int posZ = baseZ + indexZ;
						
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
				BlockPos.Mutable blockPos = new BlockPos.Mutable();
				
				int genCacheIndex = 0;
				
				for (int z = -blendRadius;
					z < BetterBiomeBlend.chunkDim + blendRadius;
					++z)
				{
					for (int x = -blendRadius;
						x < BetterBiomeBlend.chunkDim + blendRadius;
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
	generateBlendedColorChunk(IWorldReader world, int[] blendedColors, int chunkX, int chunkZ, BiomeColorType colorType)
	{
		GenCache cache = acquireGenCache();
		
		int[] rawColors = cache.colors;
		int blendRadius = cache.blendRadius;
		int blendDim = 2 * blendRadius + 1;
		
		int blendCount = blendDim * blendDim;
		int genCacheDim = chunkDim + 2 * blendRadius;
		
		int blockX = chunkX << 4;
		int blockZ = chunkZ << 4;
		
		gatherBiomeColors(colorType, world, rawColors, blockX, blockZ, blendRadius);
		
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
					
					blendedColors[colorChunkIndex] = color;
				}

				++colorChunkIndex;
				
				if (x < chunkDim - 1)
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
			
			if (z < chunkDim - 1)
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
	
	public static BlendedColorChunk
	getBlendedColorChunk(IBlockDisplayReader worldIn, int chunkX, int chunkZ, BlendedColorCache cache, BiomeColorType colorType)
	{
		BlendedColorChunk chunk = cache.getChunk(chunkX, chunkZ);
		
		if (chunk == null)
		{
			IWorldReader world = getIWorldReader(worldIn);
			
			if (world != null)
			{
				chunk = cache.newChunk(chunkX, chunkZ);
				
				generateBlendedColorChunk(world, chunk.data, chunkX, chunkZ, colorType);
				
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
		
		BlendedColorChunk chunk = getThreadLocalChunk(threadLocalWaterChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.
			
			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, waterColorCache, BiomeColorType.WATER);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalWaterChunk, chunk, waterColorCache);				
			}
			
			if (chunk != null)
			{
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
		
		BlendedColorChunk chunk = getThreadLocalChunk(threadLocalGrassChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.

			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, grassColorCache, BiomeColorType.GRASS);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalGrassChunk, chunk, grassColorCache);				
			}
			
			if (chunk != null)
			{
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
		
		BlendedColorChunk chunk = getThreadLocalChunk(threadLocalFoliageChunk, chunkX, chunkZ);
	
		if (chunk != null)
		{
			// NOTE: This could be moved further down to reduce code duplication but this is the hot code path.

			result = chunk.getColor(x, z);
		}
		else
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, foliageColorCache, BiomeColorType.FOLIAGE);
			
			if (chunk != null)
			{
				setThreadLocalChunk(threadLocalFoliageChunk, chunk, foliageColorCache);				
			}
			
			if (chunk != null)
			{
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