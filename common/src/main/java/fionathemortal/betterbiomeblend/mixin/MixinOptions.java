package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@Mixin(value = Options.class)
public abstract class MixinOptions implements BetterBiomeBlendOptions
{
    @Unique
    private OptionInstance<Integer> bbb$biomeBlendRadius;

    @Invoker("setGraphicsPresetToCustom")
    public abstract void
    bbb$setGraphicsPresetToCustom();

    @Inject(
        method = "<init>",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Options;biomeBlendRadius:Lnet/minecraft/client/OptionInstance;",
            opcode = 181,
            shift = At.Shift.AFTER))
    private void
    bbb$initBiomeBlendRadius(Minecraft minecraft, File file, CallbackInfo info)
    {
        this.bbb$biomeBlendRadius = new OptionInstance<>(
            "options.biomeBlendRadius",
            OptionInstance.noTooltip(),
            (component, integer) -> {
                int diameter = integer * 2 + 1;
                return Options.genericValueLabel(component, Component.translatable("options.biomeBlendRadius." + diameter));
            },
            new OptionInstance.IntRange(0, 14, false),
            14,
            (integer) -> {
                bbb$operateOnLevelExtractor(LevelExtractor::allChanged);
                this.bbb$setGraphicsPresetToCustom();
            });
    }

    @Inject(method = "processDumpedOptions", at = @At("HEAD"))
    private void
    bbb$onProcessDumpedOptions(Options.OptionAccess optionAccess, CallbackInfo info)
    {
        optionAccess.process("betterBiomeBlendRadius", this.bbb$biomeBlendRadius);
    }

    @Override
    public OptionInstance<Integer>
    betterBiomeBlendRadius()
    {
        return this.bbb$biomeBlendRadius;
    }

    private static void
    bbb$operateOnLevelExtractor(Consumer<LevelExtractor> consumer)
    {
        LevelExtractor levelExtractor = Minecraft.getInstance().levelExtractor;
        if (levelExtractor != null)
        {
            consumer.accept(levelExtractor);
        }
    }
}
