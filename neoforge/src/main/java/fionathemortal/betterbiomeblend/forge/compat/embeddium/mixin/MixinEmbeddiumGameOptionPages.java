package fionathemortal.betterbiomeblend.forge.compat.embeddium.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.storage.MinecraftOptionsStorage;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.gui.EmbeddiumGameOptionPages;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = EmbeddiumGameOptionPages.class, remap = false)
public class MixinEmbeddiumGameOptionPages
{
    @Unique
    private static final ResourceLocation BETTER_BIOME_BLEND_GROUP = ResourceLocation.fromNamespaceAndPath("minecraft", "better_biome_blend_group");

    @Unique
    private static final ResourceLocation BETTER_BIOME_BLEND = ResourceLocation.fromNamespaceAndPath("minecraft", "better_biome_blend");

    @Shadow
    @Final
    private static MinecraftOptionsStorage vanillaOpts;

    @Inject(
        method = "quality",
        at = @At(
            value = "INVOKE",
            target = "Lorg/embeddedt/embeddium/api/options/structure/OptionGroup;createBuilder()Lorg/embeddedt/embeddium/api/options/structure/OptionGroup$Builder;",
            ordinal = 2,
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        remap = false)
    private static void
    bbb$onQuality(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups)
    {
        groups.add(OptionGroup.createBuilder()
            .setId(BETTER_BIOME_BLEND_GROUP)
            .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                .setId(BETTER_BIOME_BLEND)
                .setName(Component.translatable("bbb.biomeBlendRadius"))
                .setTooltip(Component.translatable("bbb.biomeBlendRadius.tooltip"))
                .setControl(option -> new SliderControl(option, 0, 14, 1, ControlValueFormatter.biomeBlend()))
                .setBinding((opts, value) -> BetterBiomeBlendClient.betterBiomeBlendRadius.set(value), opts -> BetterBiomeBlendClient.betterBiomeBlendRadius.get())
                .setImpact(OptionImpact.LOW)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build())
            .build());
    }
}
