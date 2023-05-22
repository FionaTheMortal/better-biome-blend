package fionathemortal.betterbiomeblend.mixin;

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
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = new Object2ObjectArrayMap<>();

    @Unique
    public final BlendCache betterBiomeBlend$blendColorCache = new BlendCache(1024);

    @Unique
    public final ColorCache betterBiomeBlend$chunkColorCache = new ColorCache(4096 * 10);

    @Unique
    private final ThreadLocal<LocalCache> betterBiomeBlend$threadLocalCache = ThreadLocal.withInitial(LocalCache::new);

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
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci)
    {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        // TODO: Implement invalidation (?)

        betterBiomeBlend$blendColorCache.invalidateChunk(chunkX, chunkZ);
    }

    @Overwrite
    public int
    getBlockTint(BlockPos blockPosIn, ColorResolver colorResolverIn)
    {
        final int x = blockPosIn.getX();
        final int y = blockPosIn.getY();
        final int z = blockPosIn.getZ();

        final int chunkX = x >> 4;
        final int chunkY = y >> 4;
        final int chunkZ = z >> 4;

        LocalCache localCache = betterBiomeBlend$threadLocalCache.get();

        BlendChunk chunk = null;
        int        colorType;

        if (localCache.lastColorResolver == colorResolverIn)
        {
            colorType = localCache.lastColorType;

            long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

            if (localCache.lastBlendChunk.key == key)
            {
                chunk = localCache.lastBlendChunk;
            }
        }
        else
        {
            if (colorResolverIn == BiomeColors.GRASS_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.GRASS;
            }
            else if (colorResolverIn == BiomeColors.WATER_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.WATER;
            }
            else if (colorResolverIn == BiomeColors.FOLIAGE_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.FOLIAGE;
            }
            else
            {
                colorType = CustomColorResolverCompatibility.getColorType(colorResolverIn);

                if (colorType >= localCache.blendChunkCount)
                {
                    localCache.growBlendChunkArray(colorType);
                }
            }

            long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

            BlendChunk cachedChunk = localCache.blendChunks[colorType];

            if (cachedChunk.key == key)
            {
                chunk = cachedChunk;
            }
        }

        Debug.countThreadLocalChunk(chunk);

        if (chunk == null)
        {
            chunk = betterBiomeBlend$blendColorCache.getOrInitChunk(chunkX, chunkY, chunkZ, colorType);

            localCache.putChunk(chunk, colorType, colorResolverIn, betterBiomeBlend$blendColorCache);
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
                betterBiomeBlend$chunkColorCache,
                x,
                y,
                z,
                chunk.data);

            color = chunk.data[index];
        }

        return color;
    }
}
