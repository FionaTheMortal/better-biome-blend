package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.ColorChunkCache;
import fionathemortal.betterbiomeblend.ColorChunkCacheProvider;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements ColorChunkCacheProvider
{
    public final ColorChunkCache colorChunkCache = new ColorChunkCache(2048);

    @Override
    public ColorChunkCache
    getColorChunkCache()
    {
        return colorChunkCache;
    }
}
