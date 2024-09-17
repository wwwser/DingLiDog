package love.DingLi;


import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import love.DingLi.events.UpdateEvent;
import love.DingLi.utils.MSTimer;
import love.DingLi.utils.Rotation;
import love.DingLi.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import net.minecraft.util.EnumHand;

public class DingLiDog {
    public static DingLiDog INSTANCE;
    public static String target = "";
    public static Minecraft mc = Minecraft.getMinecraft();
    private static final MSTimer rightClickTimer = new MSTimer();
    public DingLiDog(){
        INSTANCE = this;
    }
    public void startClient() {
        EventManager.register(this);
    }
    @EventTarget
    private void onUpdate(UpdateEvent event){
        if (!target.isEmpty()){
            for (Entity entity : mc.world.loadedEntityList) {
                if (entity instanceof EntityLivingBase){
                    if (entity.getName().contains(target)){
                        final Rotation limitAngleChange = RotationUtils.limitAngleChange(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch), RotationUtils.toRotation(entity.getPositionVector(), false), 180f);
                        limitAngleChange.toPlayer(mc.player);
                        if (mc.player.getDistanceSq(entity)<10){

                            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
                            if (rightClickTimer.hasTimePassed(1000)){
                                mc.rightClickMouse();
                                rightClickTimer.reset();
                            }
                        }else {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(new BetterBlockPos(entity.getPosition())));
                        }
                    }
                }
            }
        }
    }
}
