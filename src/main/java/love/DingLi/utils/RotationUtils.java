package love.DingLi.utils;

import com.darkmagician6.eventapi.EventTarget;

import love.DingLi.events.PacketEvent;
import love.DingLi.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.*;

import java.util.Random;

public final class RotationUtils {
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static Random random = new Random();

    private static int keepLength;

    public static Rotation targetRotation;
    public static Rotation serverRotation;

    public static boolean keepCurrentRotation = false;

    private static double x = random.nextDouble();
    private static double y = random.nextDouble();
    private static double z = random.nextDouble();

    public static VecRotation faceBlock(final BlockPos blockPos) {
        if (blockPos == null) return null;

        VecRotation vecRotation = null;

        for (double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D) {
            for (double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D) {
                for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D) {
                    final Vec3d eyesPos = new Vec3d(mc.player.posX, mc.player.getEntityBoundingBox().minY + mc.player.getEyeHeight(), mc.player.posZ);
                    final Vec3d posVec = new Vec3d(blockPos).add(xSearch, ySearch, zSearch);

                    final double diffX = posVec.x - eyesPos.x;
                    final double diffY = posVec.y - eyesPos.y;
                    final double diffZ = posVec.z - eyesPos.z;

                    final double diffXZ = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

                    final Rotation rotation = new Rotation(
                            MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F),
                            MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))
                    );
                    final Vec3d rotationVector = getVectorForRotation(rotation);
                    final Vec3d vector = eyesPos.add(rotationVector.x * 4, rotationVector.y * 4, rotationVector.z * 4);
                    final RayTraceResult obj = mc.world.rayTraceBlocks(eyesPos, vector, false, false, true);
                    if (obj.typeOfHit == RayTraceResult.Type.BLOCK && obj.getBlockPos().equals(blockPos)) {
                        final VecRotation currentVec = new VecRotation(posVec, rotation);

                        if (vecRotation == null || getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))
                            vecRotation = currentVec;
                    }
                }
            }
        }

        if (vecRotation != null) vecRotation.getRotation().toPlayer(mc.player);
        return vecRotation;
    }
    public static boolean isFaced(final Entity targetEntity, double blockReachDistance) {
        return RaycastUtils.raycastEntity(blockReachDistance, entity -> entity == targetEntity) != null;
    }
    public static void faceBow(final Entity target, final boolean silent, final boolean predict, final float predictSize) {
        final EntityPlayerSP player = mc.player;

        final double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0) - (player.posX + (predict ? (player.posX - player.prevPosX) : 0));
        final double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? (player.posY - player.prevPosY) : 0)) - player.getEyeHeight();
        final double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0) - (player.posZ + (predict ? (player.posZ - player.prevPosZ) : 0));
        final double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        //float velocity = ModuleManager.getModule(FastBow.class).getState() ? 1F : player.getItemInUseDuration() / 20F;
        float velocity = 1F;
        velocity = (velocity * velocity + velocity * 2) / 3;
        if (velocity > 1) velocity = 1;
        final Rotation rotation = new Rotation(
                (float) (Math.atan2(posZ, posX) * 180 / Math.PI) - 90,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(velocity * velocity * velocity * velocity - 0.006F * (0.006F * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006F * posSqrt)))
        );

        if (silent)
            setTargetRotation(rotation);
        else
            limitAngleChange(new Rotation(player.rotationYaw, player.rotationPitch), rotation, 10 +
                    new Random().nextInt(6)).toPlayer(mc.player);
    }
    public static Vec3d getRandomCenter(final AxisAlignedBB bb, final boolean outborder) {
        if (outborder) {
            return new Vec3d(bb.minX + (bb.maxX - bb.minX) * (RotationUtils.x * 0.3 + 1.0), bb.minY + (bb.maxY - bb.minY) * (RotationUtils.y * 0.3 + 1.0), bb.minZ + (bb.maxZ - bb.minZ) * (RotationUtils.z * 0.3 + 1.0));
        }
        return new Vec3d(bb.minX + (bb.maxX - bb.minX) * RotationUtils.x * 0.8, bb.minY + (bb.maxY - bb.minY) * RotationUtils.y * 0.8, bb.minZ + (bb.maxZ - bb.minZ) * RotationUtils.z * 0.8);
    }
    public static Rotation toRotation(final Vec3d vec, final boolean predict) {
        final Vec3d eyesPos = new Vec3d(mc.player.posX, mc.player.getEntityBoundingBox().minY + mc.player.getEyeHeight(), mc.player.posZ);

        if (predict) eyesPos.add(mc.player.motionX, mc.player.motionY, mc.player.motionZ);

        final double diffX = vec.x - eyesPos.x;
        final double diffY = vec.y - eyesPos.y;
        final double diffZ = vec.z - eyesPos.z;

        return new Rotation(MathHelper.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F
        ), MathHelper.wrapDegrees(
                (float) (-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))
        ));
    }
    public static Vec3d getCenter(final AxisAlignedBB bb) {
        return new Vec3d(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
    }
    public static VecRotation searchCenter(final AxisAlignedBB bb, final boolean outborder, final boolean random, final boolean predict, final boolean throughWalls) {
        if (outborder) {
            final Vec3d vec3 = new Vec3d(bb.minX + (bb.maxX - bb.minX) * (x * 0.3 + 1.0), bb.minY + (bb.maxY - bb.minY) * (y * 0.3 + 1.0), bb.minZ + (bb.maxZ - bb.minZ) * (z * 0.3 + 1.0));
            return new VecRotation(vec3, toRotation(vec3, predict));
        }
        final Vec3d randomVec = new Vec3d(bb.minX + (bb.maxX - bb.minX) * x * 0.8, bb.minY + (bb.maxY - bb.minY) * y * 0.8, bb.minZ + (bb.maxZ - bb.minZ) * z * 0.8);
        final Rotation randomRotation = toRotation(randomVec, predict);
        VecRotation vecRotation = null;
        for (double xSearch = 0.15D; xSearch < 0.85D; xSearch += 0.1D) {
            for (double ySearch = 0.15D; ySearch < 1D; ySearch += 0.1D) {
                for (double zSearch = 0.15D; zSearch < 0.85D; zSearch += 0.1D) {
                    final Vec3d vec3 = new Vec3d(bb.minX + (bb.maxX - bb.minX) * xSearch, bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch);
                    final Rotation rotation = toRotation(vec3, predict);
                    if (throughWalls || isVisible(vec3)) {
                        final VecRotation currentVec = new VecRotation(vec3, rotation);
                        if (vecRotation == null || (random ? getRotationDifference(currentVec.getRotation(), randomRotation) < getRotationDifference(vecRotation.getRotation(), randomRotation) : getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation())))
                            vecRotation = currentVec;
                    }
                }
            }
        }
        return vecRotation;
    }
    public static double getRotationDifference(final Entity entity) {
        final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

        return getRotationDifference(rotation, new Rotation(mc.player.rotationYaw, mc.player.rotationPitch));
    }
    public static double getRotationDifference(final Rotation rotation) {
        return serverRotation == null ? 0D : getRotationDifference(rotation, serverRotation);
    }
    public static double getRotationDifference(final Rotation a, final Rotation b) {
        return Math.hypot(getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }
    public static Rotation limitAngleChange(final Rotation currentRotation, final Rotation targetRotation, final float turnSpeed) {
        final float yawDifference = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        final float pitchDifference = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());

        return new Rotation(
                currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)),
                currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)
                ));
    }

    private static float getAngleDifference(final float a, final float b) {
        return ((((a - b) % 360F) + 540F) % 360F) - 180F;
    }
    public static Vec3d getVectorForRotation(final Rotation rotation) {
        float yawCos = MathHelper.cos(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-rotation.getPitch() * 0.017453292F);
        float pitchSin = MathHelper.sin(-rotation.getPitch() * 0.017453292F);
        return new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }
    public static boolean isVisible(final Vec3d vec3) {
        final Vec3d eyesPos = new Vec3d(mc.player.posX, mc.player.getEntityBoundingBox().minY + mc.player.getEyeHeight(), mc.player.posZ);

        return mc.world.rayTraceBlocks(eyesPos, vec3) == null;
    }
    @EventTarget
    private void onTick(TickEvent event){
        if (targetRotation != null) {
            keepLength++;

            // Advanced Anti Cheat, huh
            if (keepLength > 15)
                reset();
        }

        if (random.nextGaussian() > 0.8D) x = Math.random();
        if (random.nextGaussian() > 0.8D) y = Math.random();
        if (random.nextGaussian() > 0.8D) z = Math.random();
    }

    @EventTarget
    private void onPacket(PacketEvent event){
        final Packet<?> packet = event.getPacket();
        if (packet instanceof CPacketPlayer) {
            final CPacketPlayer packetPlayer = (CPacketPlayer) packet;
            if (targetRotation != null && !keepCurrentRotation && (targetRotation.getYaw() != serverRotation.getYaw() || targetRotation.getPitch() != serverRotation.getPitch())) {
                packetPlayer.yaw = targetRotation.getYaw();
                packetPlayer.pitch = targetRotation.getPitch();
                packetPlayer.rotating = true;

            }
            if (packetPlayer.rotating)
                serverRotation = new Rotation(packetPlayer.getYaw(0), packetPlayer.getPitch(0));
        }
    }

    public static void setTargetRotation(final Rotation rotation) {
        if (Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;

        targetRotation = rotation;
        keepLength = 0;
    }
    public static void setToServerRotation() {
        if (serverRotation == null) return;

        targetRotation = serverRotation;
        keepLength = 0;
    }
    public static float[] getNeededRotations(final Vec3d vec, final boolean predict) {
        final Vec3d eyesPos = getEyesPos();
        if (predict) {
            eyesPos.add(RotationUtils.mc.player.motionX, RotationUtils.mc.player.motionY, RotationUtils.mc.player.motionZ);
        }
        final double diffX = vec.x - eyesPos.x;
        final double diffY = vec.y - eyesPos.y;
        final double diffZ = vec.z - eyesPos.z;
        final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        final float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        final float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }
    public static Vec3d getEyesPos() {
        return new Vec3d(RotationUtils.mc.player.posX, RotationUtils.mc.player.getEntityBoundingBox().minY + RotationUtils.mc.player.getEyeHeight(), RotationUtils.mc.player.posZ);
    }
    public static void reset() {
        keepLength = 0;
        targetRotation = null;
    }
}