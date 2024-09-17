package love.DingLi.injection.mixins;


import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static org.spongepowered.asm.lib.Opcodes.GETFIELD;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @Shadow
    protected int activeItemStackUseCount;
    @Shadow
    protected boolean isJumping;
    @Shadow
    private int jumpTicks;

    @Shadow
    public abstract boolean isHandActive();

    @Shadow
    public abstract ItemStack getActiveItemStack();

    @Shadow
    protected abstract float getJumpUpwardsMotion();

    @Shadow
    public abstract PotionEffect getActivePotionEffect(Potion potionIn);

    @Shadow
    public abstract boolean isPotionActive(Potion potionIn);

    @Shadow
    public void onLivingUpdate() {
    }

    @Shadow
    protected abstract void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos);

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract ItemStack getHeldItem(EnumHand hand);

    @Shadow
    protected abstract void updateEntityActionState();

    @Shadow
    protected abstract void handleJumpWater();

    @Shadow
    public abstract boolean isElytraFlying();

    @Shadow
    public abstract int getItemInUseCount();


    @Shadow public abstract void moveRelative(float strafe, float up, float forward, float friction);

    /**
     * Event called to override the movement direction when jumping
     */
    @Unique
    private RotationMoveEvent jumpRotationEvent;

    @Unique
    private RotationMoveEvent elytraRotationEvent;


    @Inject(
            method = "jump",
            at = @At("HEAD")
    )
    private void preMoveRelative(CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, this.rotationYaw, this.rotationPitch);
            baritone.getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
        });
    }

    @Redirect(
            method = "jump",
            at = @At(
                    value = "FIELD",
                    opcode = GETFIELD,
                    target = "net/minecraft/entity/EntityLivingBase.rotationYaw:F"
            )
    )
    private float overrideYaw(EntityLivingBase self) {
        if (self instanceof EntityPlayerSP && BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this) != null) {
            return this.jumpRotationEvent.getYaw();
        }
        return self.rotationYaw;
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/entity/EntityLivingBase.getLookVec()Lnet/minecraft/util/math/Vec3d;"
            )
    )
    private void onPreElytraMove(float strafe, float vertical, float forward, CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.elytraRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
            baritone.getGameEventHandler().onPlayerRotationMove(this.elytraRotationEvent);
            this.rotationYaw = this.elytraRotationEvent.getYaw();
            this.rotationPitch = this.elytraRotationEvent.getPitch();
        });
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/entity/EntityLivingBase.move(Lnet/minecraft/entity/MoverType;DDD)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPostElytraMove(float strafe, float vertical, float forward, CallbackInfo ci) {
        if (this.elytraRotationEvent != null) {
            this.rotationYaw = this.elytraRotationEvent.getOriginal().getYaw();
            this.rotationPitch = this.elytraRotationEvent.getOriginal().getPitch();
            this.elytraRotationEvent = null;
        }
    }

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/entity/EntityLivingBase.moveRelative(FFFF)V"
            )
    )
    private void onMoveRelative(EntityLivingBase self, float strafe, float up, float forward, float friction) {
        Optional<IBaritone> baritone = this.getBaritone();
        if (!baritone.isPresent()) {
            // If a shadow is used here it breaks on Forge
            this.moveRelative(strafe, up, forward, friction);
            return;
        }

        RotationMoveEvent event = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
        baritone.get().getGameEventHandler().onPlayerRotationMove(event);

        this.rotationYaw = event.getYaw();
        this.rotationPitch = event.getPitch();

        this.moveRelative(strafe, up, forward, friction);

        this.rotationYaw = event.getOriginal().getYaw();
        this.rotationPitch = event.getOriginal().getPitch();
    }

    @Unique
    private Optional<IBaritone> getBaritone() {
        // noinspection ConstantConditions
        if (EntityPlayerSP.class.isInstance(this)) {
            return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this));
        } else {
            return Optional.empty();
        }
    }
}
