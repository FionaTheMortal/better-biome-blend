package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.*;
import fionathemortal.betterbiomeblend.common.cache.BiomeCache;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientLevel.class)
public abstract class MixinClientWorld extends Level
{
    @Shadow
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches =
        new Object2ObjectArrayMap<>();

    @Unique
    public final BlendCache betterBiomeBlend$blendColorCache = new BlendCache(2048);

    @Unique
    public final ColorCache betterBiomeBlend$chunkColorCache = new ColorCache(4096 * 10);

    @Unique
    public final BiomeCache betterBiomeBlend$chunkBiomeCache = new BiomeCache(4096 * 10);

    @Unique
    private final ThreadLocal<BlendChunk> betterBiomeBlend$threadLocalWaterChunk =
        ThreadLocal.withInitial(
            () ->
            {
                BlendChunk chunk = new BlendChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<BlendChunk> betterBiomeBlend$threadLocalGrassChunk =
        ThreadLocal.withInitial(
            () ->
            {
                BlendChunk chunk = new BlendChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<BlendChunk> betterBiomeBlend$threadLocalFoliageChunk =
        ThreadLocal.withInitial(
            () ->
            {
                BlendChunk chunk = new BlendChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<BlendChunk> betterBiomeBlend$threadLocalGenericChunk =
        ThreadLocal.withInitial(
            () ->
            {
                BlendChunk chunk = new BlendChunk();
                chunk.acquire();
                return chunk;
            });

    protected
    MixinClientWorld(
        WritableLevelData        writableLevelData,
        ResourceKey<Level>       resourceKey,
        Holder<DimensionType>    holder,
        Supplier<ProfilerFiller> supplier,
        boolean                  bl,
        boolean                  bl2,
        long                     l,
        int                      i)
    {
        super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l, i);
    }

    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    public void
    onClearColorCaches(CallbackInfo ci)
    {
        betterBiomeBlend$blendColorCache.invalidateAll();

        int blendRadius = BetterBiomeBlendClient.getBiomeBlendRadius();

        betterBiomeBlend$chunkColorCache.invalidateAll(blendRadius);
        betterBiomeBlend$chunkBiomeCache.invalidateAll(blendRadius);
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci)
    {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        // TODO: Implement invalidation

        betterBiomeBlend$blendColorCache.invalidateChunk(chunkX, chunkZ);
        // betterBiomeBlend$chunkColorCache.invalidateSmallNeighborhood(chunkX, chunkZ);
        // betterBiomeBlend$chunkBiomeCache.invalidateSmallNeighborhood(chunkX, chunkZ);
    }

    @Unique
    private final ThreadLocal<Integer> betterBiomeBlend$lastThreadLocal =
        ThreadLocal.withInitial(
            () ->
            {
                return 0;
            });

    @Overwrite
    public int
    getBlockTint(BlockPos blockPosIn, ColorResolver colorResolverIn)
    {
        // TODO: Check access pattern
        // TODO: Does it make sense to accelerate fast path here?

        int                     colorType;
        ThreadLocal<BlendChunk> threadLocalChunk;

        if (colorResolverIn == BiomeColors.GRASS_COLOR_RESOLVER)
        {
            colorType        = BiomeColorType.GRASS;
            threadLocalChunk = betterBiomeBlend$threadLocalGrassChunk;
        }
        else if (colorResolverIn == BiomeColors.WATER_COLOR_RESOLVER)
        {
            colorType        = BiomeColorType.WATER;
            threadLocalChunk = betterBiomeBlend$threadLocalWaterChunk;
        }
        else if (colorResolverIn == BiomeColors.FOLIAGE_COLOR_RESOLVER)
        {
            colorType        = BiomeColorType.FOLIAGE;
            threadLocalChunk = betterBiomeBlend$threadLocalFoliageChunk;
        }
        else
        {
            colorType        = CustomColorResolverCompatibility.getColorType(colorResolverIn);
            threadLocalChunk = betterBiomeBlend$threadLocalGenericChunk;
        }

        int lastColorType = betterBiomeBlend$lastThreadLocal.get();

        Debug.countColorType(colorType, lastColorType);

        betterBiomeBlend$lastThreadLocal.set(colorType);

        final int x = blockPosIn.getX();
        final int y = blockPosIn.getY();
        final int z = blockPosIn.getZ();

        final int chunkX = x >> 4;
        final int chunkY = y >> 4;
        final int chunkZ = z >> 4;

        BlendChunk chunk = ColorCaching.getThreadLocalChunk(threadLocalChunk, chunkX, chunkY, chunkZ, colorType);

        Debug.countThreadLocalChunk(chunk);

        if (chunk == null)
        {
            chunk = ColorCaching.getBlendedColorChunk(
                colorType,
                chunkX,
                chunkY,
                chunkZ,
                betterBiomeBlend$blendColorCache);

            ColorCaching.setThreadLocalChunk(threadLocalChunk, chunk, betterBiomeBlend$blendColorCache);
        }

        final int blockX = x & 15;
        final int blockY = y & 15;
        final int blockZ = z & 15;

        int index = ColorCaching.getCacheArrayIndex(16, blockX, blockY, blockZ);

        int color = chunk.data[index];

        if (color == 0)
        {
            ColorBlending.generateColors(
                this,
                colorResolverIn,
                colorType,
                x,
                y,
                z,
                betterBiomeBlend$chunkColorCache,
                betterBiomeBlend$chunkBiomeCache,
                chunk.data);

            color = chunk.data[index];
        }

        return color;
    }
}
