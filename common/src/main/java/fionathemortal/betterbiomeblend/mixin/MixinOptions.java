package fionathemortal.betterbiomeblend.mixin;


import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Options.class)
public abstract class MixinOptions
{
    @Shadow
    public OptionInstance<Integer> biomeBlendRadius;

    @Inject(method = "processOptions", at = @At("HEAD"))
    private void
    injectHandle(Options.FieldAccess fieldAccess, CallbackInfo info)
    {
        fieldAccess.process("betterBiomeBlendRadius", BetterBiomeBlendClient.betterBiomeBlendRadius);
    }
}
