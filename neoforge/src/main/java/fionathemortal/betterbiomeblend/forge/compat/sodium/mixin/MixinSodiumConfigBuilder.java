package fionathemortal.betterbiomeblend.forge.compat.sodium.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SodiumConfigBuilder.class, remap = false)
public class MixinSodiumConfigBuilder
{
    @Unique
    private static ControlValueFormatter
    bbb$biomeBlendFormatter()
    {
        return (v) -> {
            if (v >= 0 && v <= 14) {
                if (v == 0) {
                    return Component.translatable("gui.none");
                } else {
                    int sv = 2 * v + 1;
                    return Component.translatable("sodium.options.biome_blend.value", new Object[]{sv, sv});
                }
            } else {
                return Component.translatable("parsing.int.invalid", new Object[]{v});
            }
        };
    }

    @Shadow
    @Final
    private StorageEventHandler vanillaStorage;

    @Inject(
        method = "buildQualityPage",
        at = @At("RETURN"),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        remap = false)
    private void
    bbb$onBuildQualityPage(ConfigBuilder builder, CallbackInfoReturnable<OptionPage> cir, OptionPageBuilder qualityPage)
    {
        qualityPage.addOptionGroup(
            builder.createOptionGroup()
                .addOption(builder.createIntegerOption(Identifier.parse("betterbiomeblend:quality.biome_blend"))
                .setStorageHandler(this.vanillaStorage)
                .setName(Component.translatable("bbb.biomeBlendRadius"))
                .setValueFormatter(bbb$biomeBlendFormatter())
                .setTooltip(Component.translatable("bbb.biomeBlendRadius.tooltip"))
                .setRange(0, 14, 1)
                .setDefaultValue(14)
                .setBinding((value) -> BetterBiomeBlendClient.betterBiomeBlendRadius.set(value), () -> BetterBiomeBlendClient.betterBiomeBlendRadius.get())
                .setImpact(OptionImpact.LOW)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)));
    }
}
