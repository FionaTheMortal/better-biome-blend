package fionathemortal.betterbiomeblend.mixin;


import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(value = Options.class)
public class MixinOptions
{
    @Shadow
    public OptionInstance<Integer> biomeBlendRadius;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("TAIL"))
    private void
    injectConstructor(Minecraft minecraft, File file, CallbackInfo info)
    {
        this.biomeBlendRadius = new OptionInstance<>(
            "options.biomeBlendRadius", OptionInstance.noTooltip(),
            (component, integer) -> {
                int i = integer * 2 + 1;
                return Options.genericValueLabel(component, Component.translatable("options.biomeBlendRadius." + i));
            }, new OptionInstance.IntRange(0, 14), 2, (integer) -> {
                Minecraft.getInstance().levelRenderer.allChanged();
            });
    }
}
