package fionathemortal.betterbiomeblend.fabric.compat.sodium.mixin;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import fionathemortal.betterbiomeblend.fabric.compat.sodium.SodiumConfig;
import fionathemortal.betterbiomeblend.fabric.compat.sodium.SourceCacheProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(value = ClientLevel.class)
public abstract class MixinClientLevel implements SourceCacheProvider
{
    @Unique
    private volatile SliceCache bbb$sodium$sourceCache;

    @Unique
    private ReentrantLock bbb$sodium$initLock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void
    onInit(CallbackInfo ci)
    {
        bbb$sodium$initLock = new ReentrantLock();
    }

    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    public void
    bbb$sodium$onClearTintCaches(CallbackInfo ci)
    {
        SliceCache cache = bbb$sodium$sourceCache;

        if (cache != null)
        {
            cache.destroy();
        }

        this.bbb$sodium$sourceCache = null;
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    bbb$sodium$onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci)
    {
        SliceCache cache = bbb$sodium$sourceCache;

        if (cache != null)
        {
            int chunkX = chunkPos.x;
            int chunkZ = chunkPos.z;

            cache.invalidateChunk(chunkX, chunkZ, true, false);
        }
    }

    @Unique
    public SliceCache
    bbb$sodium$getSourceCache()
    {
        SliceCache result = bbb$sodium$sourceCache;

        if (result == null)
        {
            bbb$sodium$initLock.lock();

            result = bbb$sodium$sourceCache;

            if (result == null)
            {
                ColorConfig config = SodiumConfig.getCurrentConfig();

                result = new SliceCache(config, config.getSourceConfig(), 512, 0);

                bbb$sodium$sourceCache = result;
            }

            bbb$sodium$initLock.unlock();
        }

        return result;
    }
}
