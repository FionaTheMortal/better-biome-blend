package fionathemortal.betterbiomeblend.mixin;


import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.LocalCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(value = Options.class)
public class MixinOptions
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
