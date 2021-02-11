package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.settings.SliderPercentageOption;

@Mixin(AbstractSlider.class)
public interface AccessorAbstractSlider
{
	@Accessor()
	void setSliderValue(double value);
}

