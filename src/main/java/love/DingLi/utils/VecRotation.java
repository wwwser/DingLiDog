package love.DingLi.utils;

import net.minecraft.util.math.Vec3d;

public class VecRotation {
    private Vec3d vec;
    private Rotation rotation;

    public VecRotation(Vec3d vec, Rotation rotation) {
        this.vec = vec;
        this.rotation = rotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Vec3d getVec() {
        return vec;
    }
}
