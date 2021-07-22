package fionathemortal.betterbiomeblend.mixin;

import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.settings.SliderPercentageOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionSlider.class)
public interface AccessorOptionSlider
{
    @Accessor()
    SliderPercentageOption getOption();
}
