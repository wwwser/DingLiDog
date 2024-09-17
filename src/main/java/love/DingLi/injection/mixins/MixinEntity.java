package love.DingLi.injection.mixins;


import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntity {

    @Shadow public float rotationPitch;
    @Shadow public float rotationYaw;
}
