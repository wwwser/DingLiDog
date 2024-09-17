package love.DingLi.injection.mixins;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FontRenderer.class)
@SideOnly(Side.CLIENT)
public class MixinFontRenderer {

//    @ModifyVariable(method = "renderString", at = @At("HEAD"), ordinal = 0)
//    private String renderString(final String string) {
//        if (string == null || ClientBase.INSTANCE.eventManager == null)
//            return string;
//
//        final TextEvent textEvent = new TextEvent(string);
//
//        EventManager.call(textEvent);
//        return textEvent.getText();
//    }
//
//    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), ordinal = 0)
//    private String getStringWidth(final String string) {
//        if (string == null || Client.eventManager == null)
//            return string;
//
//        final TextEvent textEvent = new TextEvent(string);
//        EventManager.call(textEvent);
//        return textEvent.getText();
//    }
}