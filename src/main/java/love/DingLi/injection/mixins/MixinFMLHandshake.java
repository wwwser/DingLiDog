package love.DingLi.injection.mixins;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

@Mixin(value = FMLHandshakeMessage.class, remap = false)
public class MixinFMLHandshake {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static FMLProxyPacket makeCustomChannelRegistration(Set<String> channels)
    {
        String salutation =  Joiner.on((char) 0).join(Iterables.concat(Arrays.asList("FML|HS", "FML", "FML|MP", "elementserver", "epicofelementmod", "dragoncore:main", "ddqt", "nbtedit", "systemvelocity", "systemhall", "FORGE", "statusbar", "mobends", "armourers")));
        FMLProxyPacket proxy = new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(salutation.getBytes(StandardCharsets.UTF_8))), "REGISTER");
        return proxy;
    }
}
