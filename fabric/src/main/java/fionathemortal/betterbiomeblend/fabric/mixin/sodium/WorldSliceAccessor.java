package fionathemortal.betterbiomeblend.fabric.mixin.sodium;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.Level;

@Mixin(WorldSlice.class)
public interface WorldSliceAccessor
{
    @Accessor("world")
    Level getWorld();
}
