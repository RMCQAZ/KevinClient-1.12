package kevin.utils;

import kevin.KevinClient;
import kevin.event.Listenable;
import kevin.module.modules.player.FastUse;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import javax.annotation.Nonnull;
import java.util.Random;

public final class RotationUtils extends Mc implements Listenable {

    public static final Random random = new Random();

    public static int keepLength;

    public static Rotation targetRotation;
    public static Rotation serverRotation = new Rotation(0F, 0F);

    public static boolean keepCurrentRotation = false;

    public static double x = random.nextDouble();
    public static double y = random.nextDouble();
    public static double z = random.nextDouble();

    public static Rotation getRotationsEntity(EntityLivingBase entity) {
        return RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight() - 0.4, entity.posZ);
    }

    public static Rotation getRotations(double posX, double posY, double posZ) {
        EntityPlayerSP player = RotationUtils.getMc().player;
        double x = posX - player.posX;
        double y = posY - (player.posY + (double)player.getEyeHeight());
        double z = posZ - player.posZ;
        double dist = MathHelper.sqrt(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / 3.141592653589793) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / 3.141592653589793));
        return new Rotation(yaw,pitch);
    }

    public static Rotation getRotations(Entity ent) {
        double x = ent.posX;
        double z = ent.posZ;
        double y = ent.posY + (double)(ent.getEyeHeight() / 2.0f);
        return RotationUtils.getRotationFromPosition(x, z, y);
    }
    public static Rotation getRotationFromPosition(double x, double z, double y) {
        double xDiff = x - getMc().player.posX;
        double zDiff = z - getMc().player.posZ;
        double yDiff = y - getMc().player.posY - 1.2;
        double dist = MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);
        float yaw = (float)(Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(- Math.atan2(yDiff, dist) * 180.0 / Math.PI);
        return new Rotation(yaw,pitch);
    }

    /**
     * Face block
     *
     * @param blockPos target block
     */
    public static VecRotation faceBlock(final BlockPos blockPos) {
        if (blockPos == null)
            return null;

        VecRotation vecRotation = null;

        for (double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D) {
            for (double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D) {
                for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D) {
                    final Vec3d eyesPos = new Vec3d(getMc().player.posX, getMc().player.getEntityBoundingBox().minY + getMc().player.getEyeHeight(), getMc().player.posZ);
                    final Vec3d posVec = new Vec3d(blockPos).addVector(xSearch, ySearch, zSearch);
                    final double dist = eyesPos.distanceTo(posVec);

                    final double diffX = posVec.x - eyesPos.x;
                    final double diffY = posVec.y - eyesPos.y;
                    final double diffZ = posVec.z - eyesPos.z;

                    final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                    final Rotation rotation = new Rotation(
                            wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F),
                            wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))
                    );

                    final Vec3d rotationVector = getVectorForRotation(rotation);
                    final Vec3d vector = eyesPos.addVector(rotationVector.x * dist, rotationVector.y * dist,
                            rotationVector.z * dist);
                    final RayTraceResult obj = getMc().world.rayTraceBlocks(eyesPos, vector, false,
                            false, true);

                    if (obj != null && obj.typeOfHit == RayTraceResult.Type.BLOCK) {
                        final VecRotation currentVec = new VecRotation(posVec, rotation);

                        if (vecRotation == null || getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))
                            vecRotation = currentVec;
                    }
                }
            }
        }

        return vecRotation;
    }

    public static void faceBow(final Entity target, final boolean silent, final boolean predict, final float predictSize) {
        final EntityPlayerSP player = getMc().player;

        final double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0) - (player.posX + (predict ? (player.posX - player.prevPosX) : 0));
        final double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? (player.posY - player.prevPosY) : 0)) - player.getEyeHeight();
        final double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0) - (player.posZ + (predict ? (player.posZ - player.prevPosZ) : 0));
        final double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        float velocity = (KevinClient.moduleManager.getModule("FastUse").getState() && ((FastUse) KevinClient.moduleManager.getModule("FastUse")).getFastBow().get()) ? 1F : (player.getHeldItem(EnumHand.MAIN_HAND).getMaxItemUseDuration() - player.getItemInUseCount()) / 20F;
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
                    new Random().nextInt(6)).toPlayer(getMc().player);
    }

    public static Rotation toRotation(final Vec3d vec, final boolean predict) {
        final Vec3d eyesPos = new Vec3d(getMc().player.posX, getMc().player.getEntityBoundingBox().minY +
                getMc().player.getEyeHeight(), getMc().player.posZ);

        if (predict)
            eyesPos.addVector(getMc().player.motionX, getMc().player.motionY, getMc().player.motionZ);

        final double diffX = vec.x - eyesPos.x;
        final double diffY = vec.y - eyesPos.y;
        final double diffZ = vec.z - eyesPos.z;

        return new Rotation(wrapAngleTo180_float(
                (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F
        ), wrapAngleTo180_float(
                (float) (-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))
        ));
    }

    public static Float wrapAngleTo180_float(Float angle) {
        var value = angle % 360.0f;
        if (value >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    public static Vec3d getCenter(final AxisAlignedBB bb) {
        return new Vec3d(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
    }

    public static VecRotation searchCenter(final AxisAlignedBB bb, final boolean outborder, final boolean random,
                                           final boolean predict, final boolean throughWalls, final float distance) {
        if (outborder) {
            final Vec3d vec3 = new Vec3d(bb.minX + (bb.maxX - bb.minX) * (x * 0.3 + 1.0), bb.minY + (bb.maxY - bb.minY) * (y * 0.3 + 1.0), bb.minZ + (bb.maxZ - bb.minZ) * (z * 0.3 + 1.0));
            return new VecRotation(vec3, toRotation(vec3, predict));
        }

        final Vec3d randomVec = new Vec3d(bb.minX + (bb.maxX - bb.minX) * x * 0.8, bb.minY + (bb.maxY - bb.minY) * y * 0.8, bb.minZ + (bb.maxZ - bb.minZ) * z * 0.8);
        final Rotation randomRotation = toRotation(randomVec, predict);

        final Vec3d eyes = getMc().player.getPositionEyes(1F);

        VecRotation vecRotation = null;

        for(double xSearch = 0.15D; xSearch < 0.85D; xSearch += 0.1D) {
            for (double ySearch = 0.15D; ySearch < 1D; ySearch += 0.1D) {
                for (double zSearch = 0.15D; zSearch < 0.85D; zSearch += 0.1D) {
                    final Vec3d vec3 = new Vec3d(bb.minX + (bb.maxX - bb.minX) * xSearch,
                            bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch);
                    final Rotation rotation = toRotation(vec3, predict);
                    final double vecDist = eyes.distanceTo(vec3);

                    if (vecDist > distance)
                        continue;

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

        return getRotationDifference(rotation, new Rotation(getMc().player.rotationYaw, getMc().player.rotationPitch));
    }

    public static double getRotationDifference(final Rotation rotation) {
        return serverRotation == null ? 0D : getRotationDifference(rotation, serverRotation);
    }

    public static double getRotationDifference(final Rotation a, final Rotation b) {
        return Math.hypot(getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }

    @Nonnull
    public static Rotation limitAngleChange(final Rotation currentRotation, final Rotation targetRotation, final float turnSpeed) {
        final float yawDifference = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        final float pitchDifference = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());

        return new Rotation(
                currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)),
                currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)
                ));
    }

    public static float getAngleDifference(final float a, final float b) {
        return ((((a - b) % 360F) + 540F) % 360F) - 180F;
    }

    public static Vec3d getVectorForRotation(final Rotation rotation) {
        float yawCos = (float) Math.cos(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float yawSin = (float) Math.sin(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float pitchCos = (float) -Math.cos(-rotation.getPitch() * 0.017453292F);
        float pitchSin = (float) Math.sin(-rotation.getPitch() * 0.017453292F);
        return new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public static boolean isFaced(final Entity targetEntity, double blockReachDistance) {
        return RaycastUtils.raycastEntity(blockReachDistance, entity -> targetEntity != null && targetEntity.equals(entity)) != null;
    }

    public static boolean isVisible(final Vec3d vec3) {
        final Vec3d eyesPos = new Vec3d(getMc().player.posX, getMc().player.getEntityBoundingBox().minY + getMc().player.getEyeHeight(), getMc().player.posZ);

        return getMc().world.rayTraceBlocks(eyesPos, vec3) == null;
    }

    public static void setTargetRotation(final Rotation rotation, final int keepLength) {
        if (Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;

        rotation.fixedSensitivity(getMc().gameSettings.mouseSensitivity);
        targetRotation = rotation;
        RotationUtils.keepLength = keepLength;
    }

    public static void setTargetRotation(final Rotation rotation) {
        setTargetRotation(rotation, 0);
    }

    public static void reset() {
        keepLength = 0;
        targetRotation = null;
    }

    @Override
    public boolean handleEvents() {return true;}
}

