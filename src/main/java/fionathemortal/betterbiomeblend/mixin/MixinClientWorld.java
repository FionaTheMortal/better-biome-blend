package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.common.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
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
    private final BlendCache betterBiomeBlend$blendColorCache = new BlendCache(2048);

    @Unique
    private final ColorCache betterBiomeBlend$chunkColorCache = new ColorCache(512);

    @Unique
    private final BiomeCache betterBiomeBlend$chunkBiomeCache = new BiomeCache(32);

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
        WritableLevelData        p_46450_,
        ResourceKey<Level>       p_46451_,
        final DimensionType      p_46452_,
        Supplier<ProfilerFiller> p_46453_,
        boolean                  p_46454_,
        boolean                  p_46455_,
        long                     p_46456_)
    {
        super(p_46450_, p_46451_, p_46452_, p_46453_, p_46454_, p_46455_, p_46456_);
    }

    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    public void
    onClearColorCaches(CallbackInfo ci)
    {
        betterBiomeBlend$blendColorCache.invalidateAll();
        betterBiomeBlend$chunkColorCache.invalidateAll();
        betterBiomeBlend$chunkBiomeCache.invalidateAll();
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci)
    {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        betterBiomeBlend$blendColorCache.invalidateChunk(chunkX, chunkZ);
        betterBiomeBlend$chunkColorCache.invalidateSmallNeighborhood(chunkX, chunkZ);
        betterBiomeBlend$chunkBiomeCache.invalidateSmallNeighborhood(chunkX, chunkZ);
    }

    @Overwrite
    public int
    getBlockTint(BlockPos blockPosIn, ColorResolver colorResolverIn)
    {
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

        int x = blockPosIn.getX();
        int y = blockPosIn.getY();
        int z = blockPosIn.getZ();

        int chunkX = x >> 4;
        int chunkY = y >> 4;
        int chunkZ = z >> 4;

        BlendChunk chunk = ColorCaching.getThreadLocalChunk(threadLocalChunk, chunkX, chunkY, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = ColorCaching.getBlendedColorChunk(
                this,
                colorResolverIn,
                colorType,
                chunkX,
                chunkY,
                chunkZ,
                betterBiomeBlend$blendColorCache,
                betterBiomeBlend$chunkColorCache,
                betterBiomeBlend$chunkBiomeCache);

            ColorCaching.setThreadLocalChunk(threadLocalChunk, chunk, betterBiomeBlend$blendColorCache);
        }

        int result = chunk.getColor(x, y, z);

        return result;
    }
}
