package org.ricey_yam.lynxmind.client.utils.game_ext;

import baritone.api.utils.Rotation;
import baritone.api.utils.VecUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;

public class TransformUtils {
    /**
     * 将 Minecraft 原始偏航角（可能大于360或小于0）规范化到 -180 到 180 的范围。
     * @param yaw 原始偏航角
     * @return 规范化后的偏航角 (-180.0F <= yaw < 180.0F)
     */
    public static float normalizeYaw180(float yaw) {
        yaw = yaw % 360.0F;
        if (yaw >= 180.0F) {
            yaw -= 360.0F;
        } else if (yaw < -180.0F) {
            yaw += 360.0F;
        }
        return yaw;
    }

    /// 计算旋转角度
    public static Rotation calcLookRotationFromVec3d(PlayerEntity player, BlockPos to) {
        var vec3dForm = player.getEyePos();
        var vec3dTo = VecUtils.getBlockPosCenter(to);
        return getRotation(vec3dForm, vec3dTo);
    }

    private static Rotation getRotation(Vec3d vec3dForm, Vec3d vec3dTo) {
        var diff = vec3dTo.subtract(vec3dForm);
        var distance = diff.length();
        var xzDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, xzDistance));
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;

        return new Rotation(yaw, pitch).normalize();
    }

    /// 是否玩家看向某个位置
    public static boolean isLookingAt(BlockPos pos) {
        var baritone = BaritoneManager.getClientBaritone();
        if (baritone == null) return false;

        var ctx = baritone.getPlayerContext();

        var player = ctx.player();

        Vec3d eyePosition = player.getEyePos();

        Vec3d targetCenter = VecUtils.getBlockPosCenter(pos);

        Rotation idealRotation = getRotation(eyePosition, targetCenter);

        Rotation currentRotation = ctx.playerRotations();

        double yawDiff = Math.abs(normalizeYaw180(idealRotation.getYaw() - currentRotation.getYaw()));
        double pitchDiff = Math.abs(idealRotation.getPitch() - currentRotation.getPitch());

        double totalDiff = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        return totalDiff < 4D;
    }

}
