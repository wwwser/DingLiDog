package love.DingLi.injection.mixins;


import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.type.EventState;
import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.types.EventType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Shadow
    public Channel channel;

    @Shadow
    @Final
    public EnumPacketDirection direction;

    @Inject(
            method = "dispatchPacket",
            at = @At("HEAD")
    )
    private void preDispatchPacket(Packet<?> inPacket, final GenericFutureListener<? extends Future<? super Void>>[] futureListeners, CallbackInfo ci) {
        if (this.direction != EnumPacketDirection.CLIENTBOUND) {
            return;
        }

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((NetworkManager) (Object) this, EventState.PRE, inPacket));
            }
        }
    }

    @Inject(
            method = "dispatchPacket",
            at = @At("RETURN")
    )
    private void postDispatchPacket(Packet<?> inPacket, final GenericFutureListener<? extends Future<? super Void>>[] futureListeners, CallbackInfo ci) {
        if (this.direction != EnumPacketDirection.CLIENTBOUND) {
            return;
        }

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((NetworkManager) (Object) this, EventState.POST, inPacket));
            }
        }
    }

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        final love.DingLi.events.PacketEvent event = new love.DingLi.events.PacketEvent(EventType.RECEIVE,packet);
        EventManager.call(event);

        if (event.isCancelled())
            callback.cancel();
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {

        final love.DingLi.events.PacketEvent event = new love.DingLi.events.PacketEvent(EventType.SEND,packet);
        EventManager.call(event);

        if (event.isCancelled())
            callback.cancel();

    }

    @Inject(
            method = "channelRead0*",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/Packet.processPacket(Lnet/minecraft/network/INetHandler;)V"
            )
    )
    private void preProcessPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (this.direction != EnumPacketDirection.CLIENTBOUND) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((NetworkManager) (Object) this, EventState.PRE, packet));
            }
        }
    }

    @Inject(
            method = "channelRead0*",
            at = @At("RETURN")
    )
    private void postProcessPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!this.channel.isOpen() || this.direction != EnumPacketDirection.CLIENTBOUND) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((NetworkManager) (Object) this, EventState.POST, packet));
            }
        }
    }

}