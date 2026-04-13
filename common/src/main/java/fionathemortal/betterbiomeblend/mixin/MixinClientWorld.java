package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.common.*;
import fionathemortal.betterbiomeblend.common.cache.LocalCache;
import fionathemortal.betterbiomeblend.common.cache.Slice;
import fionathemortal.betterbiomeblend.common.util.Util;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
@Mixin(value = ClientLevel.class)
public abstract class MixinClientWorld
{
    @Shadow
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = new Object2ObjectArrayMap<>();

    @Unique
    private ThreadLocal<LocalCache> bbb$threadLocalCache;

    @Unique
    private ReentrantLock bbb$initLock;

    @Unique
    private volatile ColorSource bbb$colorSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void
    bbb$onInit(CallbackInfo ci)
    {
        bbb$initLock = new ReentrantLock();

        bbb$threadLocalCache = ThreadLocal.withInitial(LocalCache::new);
    }

    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    public void
    bbb$onClearTintCaches(CallbackInfo ci)
    {
        ColorSource colorSource = bbb$colorSource;

        if (colorSource != null)
        {
            colorSource.destroy();
        }

        this.bbb$colorSource = null;
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    bbb$onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci)
    {
        ColorSource colorSource = bbb$colorSource;

        if (colorSource != null)
        {
            int chunkX = chunkPos.x;
            int chunkZ = chunkPos.z;

            colorSource.invalidateChunk(chunkX, chunkZ);
        }
    }

    @Unique
    private ColorSource
    bbb$getColorSource()
    {
        ColorSource result = this.bbb$colorSource;

        if (result == null)
        {
            bbb$initLock.lock();

            result = this.bbb$colorSource;

            if (result == null)
            {
                ColorConfig config = ColorConfig.getCurrentConfig();

                result = new ColorSource(config, (ClientLevel)(Object)this);

                this.bbb$colorSource = result;
            }

            bbb$initLock.unlock();
        }

        return result;
    }

    @Overwrite
    public int
    getBlockTint(BlockPos colorPos, ColorResolver colorResolver)
    {
        final int blockX = colorPos.getX();
        final int blockY = colorPos.getY();
        final int blockZ = colorPos.getZ();

        final int chunkX = Util.blockToChunk(blockX);
        final int chunkY = Util.blockToChunk(blockY);
        final int chunkZ = Util.blockToChunk(blockZ);

        final int localX = Util.getBlockInChunk(blockX);
        final int localY = Util.getBlockInChunk(blockY);
        final int localZ = Util.getBlockInChunk(blockZ);

        LocalCache localCache = bbb$threadLocalCache.get();

        int   colorType = localCache.getColorType(colorResolver);
        Slice slice     = localCache.getSlice(chunkX, chunkY, chunkZ, colorType);

        if (slice == null)
        {
            ColorSource colorSource = bbb$getColorSource();

            slice = colorSource.getSlice(chunkX, chunkY, chunkZ, colorType);

            if (slice == null)
            {
                slice = colorSource.genSlice(chunkX, chunkY, chunkZ, colorType, colorResolver);
            }

            localCache.putSlice(colorSource, slice, colorType, colorResolver);
        }

        return slice.getColor(localX, localY, localZ);
    }
}
