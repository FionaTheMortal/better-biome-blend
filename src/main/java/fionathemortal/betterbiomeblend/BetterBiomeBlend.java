package fionathemortal.betterbiomeblend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fionathemortal.betterbiomeblend.mixin.AccessorChunkRenderCache;
import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.AbstractOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.CubeCoordinateIterator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlend 
{
	public static final String MOD_ID = "betterbiomeblend";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	public static final int chunkDim = 16;
	
	public static final ThreadLocal<BlendedColorChunk> threadLocalWaterChunk   = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<BlendedColorChunk> threadLocalGrassChunk   = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	public static final ThreadLocal<BlendedColorChunk> threadLocalFoliageChunk = ThreadLocal.withInitial(() -> { BlendedColorChunk chunk = new BlendedColorChunk(); chunk.acquire(); return chunk; });
	
	public static final BlendedColorCache waterColorCache   = new BlendedColorCache(512);
	public static final BlendedColorCache grassColorCache   = new BlendedColorCache(512);
	public static final BlendedColorCache foliageColorCache = new BlendedColorCache(512);

	public static final List<int[]> freeGenCaches = new ArrayList<int[]>();

	public static int blendRadius = 14;
	
	public static final SliderPercentageOption BIOME_BLEND_RADIUS = new SliderPercentageOption("options.biomeBlendRadius", 0.0D, 14.0D, 1.0F, (settings) -> {
		return (double)settings.biomeBlendRadius;
	}, (settings, optionValues) -> {
		settings.biomeBlendRadius = MathHelper.clamp((int)optionValues.doubleValue(), 0, 14);
		Minecraft.getInstance().worldRenderer.loadRenderers();
	}, (settings, optionValues) -> {
		double d0 = optionValues.get(settings);
		int i = (int)d0 * 2 + 1;
		
		return new TranslationTextComponent("options.generic_value", new TranslationTextComponent("options.biomeBlendRadius"), new TranslationTextComponent("options.biomeBlendRadius." + i));
	});

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
    		
    		List<? extends IGuiEventListener> children = videoSettingsScreen.getEventListeners();
    		
    		for (IGuiEventListener child : children)
    		{
    			if (child instanceof OptionsRowList)
    			{
    				OptionsRowList rowList = (OptionsRowList)child;
    				
    				List<OptionsRowList.Row> rowListEntries = rowList.getEventListeners();
    				
    				for (int index = 0;
    					index < rowListEntries.size();
    					++index)
    				{
    					OptionsRowList.Row row = rowListEntries.get(index);
    					
        				List<? extends IGuiEventListener> rowChildren = row.getEventListeners();

        				if (rowChildren.size() == 1)
        				{
        					IGuiEventListener rowChild = rowChildren.get(0);
        					
        					if (rowChild instanceof AccessorOptionSlider)
        					{
        						AccessorOptionSlider accessor = (AccessorOptionSlider)rowChild;
        						
        						if (accessor.getOption() == AbstractOption.BIOME_BLEND_RADIUS)
        						{
        							OptionsRowList.Row newRow = OptionsRowList.Row.create(screen.getMinecraft().gameSettings, screen.width, BIOME_BLEND_RADIUS);
        							
        							rowListEntries.set(index, newRow);
        						}
        					}
        				}
    				}
    			}
    		}
    	}
    }
    
	public
	BetterBiomeBlend()
	{
		MinecraftForge.EVENT_BUS.register(BetterBiomeBlend.class);
	}
	
	public static void
	chunkWasLoaded(IChunk chunk)
	{
		int x = chunk.getPos().x;
		int z = chunk.getPos().z;
		
		waterColorCache.invalidateNeighbourhood(x, z);
		grassColorCache.invalidateNeighbourhood(x, z);
		foliageColorCache.invalidateNeighbourhood(x, z);
	}
	
	public static GenCache
	getGenCache()
	{
		GenCache result = new GenCache();
		
		synchronized(freeGenCaches)
		{
			if (freeGenCaches.size() > 0)
			{
				result.colors = freeGenCaches.remove(freeGenCaches.size() - 1);
			}
			
			result.blendRadius = blendRadius;
		}
		
		if (result.colors == null)
		{
			int blendRadius = result.blendRadius;
			int genCacheDim = chunkDim + 2 * blendRadius;
			
			result.colors = new int[genCacheDim * genCacheDim];
		}
		
		return result;
	}
	
	public static void
	freeGenCache(GenCache value)
	{
		synchronized(freeGenCaches)
		{
			if (value.blendRadius == blendRadius)
			{
				freeGenCaches.add(value.colors);						
			}
		}
	}
	
	public static void
	setBlendRadius(int newBlendRadius)
	{
		if (blendRadius != newBlendRadius)
		{
			synchronized (freeGenCaches)
			{
				blendRadius = newBlendRadius;
				freeGenCaches.clear();
			}
		}
	}
	
	public static BlendedColorChunk
	getThreadLocalChunk(ThreadLocal<BlendedColorChunk> threadLocal, int chunkX, int chunkZ)
	{
		BlendedColorChunk chunk = null;
		BlendedColorChunk local = threadLocal.get();
		
		long key = BlendedColorCache.getChunkKey(chunkX, chunkZ);
		
		if (local.key == key)
		{
			chunk = local;
		}
		
		return chunk;
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
		GenCache cache = getGenCache();
		
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
		
		freeGenCache(cache);
	}
	
	public static BlendedColorChunk
	getBlendedColorChunk(IBlockDisplayReader worldIn, int chunkX, int chunkZ, BlendedColorCache cache, BiomeColorType colorType)
	{
		BlendedColorChunk chunk = cache.getChunkFromHash(chunkX, chunkZ);
		
		if (chunk == null)
		{
			chunk = cache.allocateChunk(chunkX, chunkZ);
			
			World world = null;
			
			if (worldIn instanceof AccessorChunkRenderCache)
			{
				world = ((AccessorChunkRenderCache)worldIn).getWorld();
			}
			else
			{
				world = (World)worldIn;
			}
			
			generateBlendedColorChunk(world, chunk.data, chunkX, chunkZ, colorType);
			
			cache.addChunkToHash(chunk);
		}
		
		return chunk;
	}
	
	public static BlendedColorChunk
	getBlendedWaterColorChunk(IBlockDisplayReader world, int chunkX, int chunkZ)
	{
		BlendedColorChunk chunk; // = getThreadLocalChunk(threadLocalWaterChunk, chunkX, chunkZ);
		
		// if (chunk == null)
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, waterColorCache, BiomeColorType.WATER);
			
			// setThreadLocalChunk(threadLocalWaterChunk, chunk, waterColorCache);
			
			chunk.release();
		}
		
		return chunk;
	}
	
	public static BlendedColorChunk
	getBlendedGrassColorChunk(IBlockDisplayReader world, int chunkX, int chunkZ)
	{
		BlendedColorChunk chunk = getThreadLocalChunk(threadLocalGrassChunk, chunkX, chunkZ);
		
		if (chunk == null)
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, grassColorCache, BiomeColorType.GRASS);
			
			setThreadLocalChunk(threadLocalGrassChunk, chunk, grassColorCache);
		}

		return chunk;
	}

	public static BlendedColorChunk
	getBlendedFoliageColorChunk(IBlockDisplayReader world, int chunkX, int chunkZ)
	{
		BlendedColorChunk chunk = getThreadLocalChunk(threadLocalFoliageChunk, chunkX, chunkZ);
		
		if (chunk == null)
		{
			chunk = getBlendedColorChunk(world, chunkX, chunkZ, foliageColorCache, BiomeColorType.FOLIAGE);
			
			setThreadLocalChunk(threadLocalFoliageChunk, chunk, foliageColorCache);
		}

		return chunk;
	}
}