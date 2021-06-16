package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.settings.SliderPercentageOption;

@Mixin(OptionSlider.class)
public interface AccessorOptionSlider
{
    @Accessor()
    SliderPercentageOption getOption();
}
