package love.DingLi.injection.mixins;


import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.type.EventState;
import baritone.behavior.LookBehavior;
import com.darkmagician6.eventapi.EventManager;

import love.DingLi.DingLiDog;
import love.DingLi.events.UpdateEvent;
import love.DingLi.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer{
    @Shadow
    private boolean serverSprintState;
    @Shadow
    public int sprintingTicksLeft;
    @Shadow
    public float timeInPortal;
    @Shadow
    public float prevTimeInPortal;
    @Shadow
    public MovementInput movementInput;
    @Shadow
    public float horseJumpPower;
    @Shadow
    public int horseJumpPowerCounter;
    @Shadow
    @Final
    public NetHandlerPlayClient connection;
    @Shadow
    protected int sprintToggleTimer;
    @Shadow
    protected Minecraft mc;
    @Shadow
    private boolean serverSneakState;
    @Shadow
    private double lastReportedPosX;
    @Shadow
    private int positionUpdateTicks;
    @Shadow
    private double lastReportedPosY;
    @Shadow
    private double lastReportedPosZ;
    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
    @Shadow
    private int autoJumpTime;
    @Shadow
    private boolean wasFallFlying;
    @Shadow
    private boolean prevOnGround;
    @Shadow
    private boolean autoJumpEnabled;

    @Shadow
    public abstract void playSound(SoundEvent soundIn, float volume, float pitch);

    @Shadow
    public abstract void setSprinting(boolean sprinting);

    @Shadow
    protected abstract boolean pushOutOfBlocks(double x, double y, double z);

    @Shadow
    public abstract void sendPlayerAbilities();

    @Shadow
    protected abstract void sendHorseJump();

    @Shadow
    public abstract boolean isRidingHorse();

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    @Shadow
    public abstract void closeScreen();

    @Shadow
    public abstract boolean isHandActive();

    @Shadow
    public abstract float getHorseJumpPower();

    @Shadow
    protected abstract void updateAutoJump(float p_189810_1_, float p_189810_2_);



    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void test(CallbackInfo callback) {
        EventManager.call(new UpdateEvent());
    }

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendChatMessage(String msg, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(msg);
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone == null) {
            return;
        }
        baritone.getGameEventHandler().onSendChatMessage(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        if (msg.startsWith(".mine") && msg.length() > 1) {
            String[] split = msg.split(" ");
            if (split.length == 2){
                ChatUtils.send("丁力爱你哦~ 您当前的目标是:" + split[1] + " (所有实体名字包含此字符都会作为目标,.mine为关闭)");
                DingLiDog.target = split[1];
            }
            else {
                ChatUtils.send("丁力爱你哦~ 已关闭自动挖矿(所有实体名字包含此字符都会作为目标,.mine为关闭)");
                DingLiDog.target = "";
            }

            ci.cancel();
        }
    }

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/entity/AbstractClientPlayer.onUpdate()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPreUpdate(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
        }
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "FIELD",
                    target = "net/minecraft/entity/player/PlayerCapabilities.allowFlying:Z"
            )
    )
    private boolean isAllowFlying(PlayerCapabilities capabilities) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone == null) {
            return capabilities.allowFlying;
        }
        return !baritone.getPathingBehavior().isPathing() && capabilities.allowFlying;
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/settings/KeyBinding.isKeyDown()Z"
            )
    )
    private boolean isKeyDown(KeyBinding keyBinding) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone == null) {
            return keyBinding.isKeyDown();
        }
        SprintStateEvent event = new SprintStateEvent();
        baritone.getGameEventHandler().onPlayerSprintState(event);
        if (event.getState() != null) {
            return event.getState();
        }
        if (baritone != BaritoneAPI.getProvider().getPrimaryBaritone()) {
            // hitting control shouldn't make all bots sprint
            return false;
        }
        return keyBinding.isKeyDown();
    }

    @Inject(
            method = "updateRidden",
            at = @At(
                    value = "HEAD"
            )
    )
    private void updateRidden(CallbackInfo cb) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone != null) {
            ((LookBehavior) baritone.getLookBehavior()).pig();
        }
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/item/ItemElytra.isUsable(Lnet/minecraft/item/ItemStack;)Z"
            )
    )
    private boolean isElytraUsable(ItemStack stack) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        if (baritone != null && baritone.getPathingBehavior().isPathing()) {
            return false;
        }
        return ItemElytra.isUsable(stack);
    }
}
