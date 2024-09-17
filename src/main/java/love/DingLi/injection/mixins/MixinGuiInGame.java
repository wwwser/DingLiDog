package love.DingLi.injection.mixins;

import com.darkmagician6.eventapi.EventManager;
import love.DingLi.events.Render2DEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiInGame extends Gui {

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        EventManager.call(new Render2DEvent());
    }
}
