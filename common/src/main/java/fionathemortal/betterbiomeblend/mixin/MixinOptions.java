package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.common.accessor.MixinOptionsAccessor;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(value = Options.class)
public abstract class MixinOptions implements MixinOptionsAccessor
{
    @Unique
    public int bbb$betterBiomeBlendRadius;

    @Inject(method = "processOptions", at = @At("HEAD"))
    private void
    bbb$onProcessOptions(Options.FieldAccess fieldAccess, CallbackInfo info)
    {
        this.bbb$betterBiomeBlendRadius = fieldAccess.process(
            "betterBiomeBlendRadius",
            bbb$betterBiomeBlendRadius);
    }

    @Override
    public int
    bbb$getBetterBiomeBlendRadius()
    {
        return this.bbb$betterBiomeBlendRadius;
    }

    @Override
    public void
    bbb$setBetterBiomeBlendRadius(int value)
    {
        this.bbb$betterBiomeBlendRadius = value;
    }
}
