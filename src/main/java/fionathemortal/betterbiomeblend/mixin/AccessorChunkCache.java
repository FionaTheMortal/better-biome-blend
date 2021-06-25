package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

@Mixin(ChunkCache.class)
public interface AccessorChunkCache
{
    @Accessor()
    World getWorld();
}
