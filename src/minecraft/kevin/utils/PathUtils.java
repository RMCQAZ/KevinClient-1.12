package kevin.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.List;

public final class PathUtils extends Mc {

    private static boolean canPassThrow(BlockPos pos) {
        IBlockState blockState = Minecraft.getMinecraft().world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        Block block = blockState.getBlock();
        return block.getMaterial(blockState) == Material.AIR || block.getMaterial(blockState) == Material.PLANTS || block.getMaterial(blockState) == Material.VINE || block == Blocks.LADDER || block == Blocks.WATER || block == Blocks.FLOWING_WATER || block == Blocks.WALL_SIGN || block == Blocks.STANDING_SIGN;
    }

    public static List<Vec3d> findBlinkPath2(double curX, double curY, double curZ, final double tpX, final double tpY, final double tpZ, final double dashDistance) {
        Vec3d topFrom=new Vec3d(curX,curY,curZ);
        Vec3d to=new Vec3d(tpX,tpY,tpZ);

        if (!canPassThrow(new BlockPos(topFrom))) {
            topFrom = topFrom.addVector(0, 1, 0);
        }
        AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
        pathfinder.compute();

        int i = 0;
        Vec3d lastLoc = null;
        Vec3d lastDashLoc = null;
        ArrayList<Vec3d> path = new ArrayList<>();
        ArrayList<Vec3d> pathFinderPath = pathfinder.getPath();
        for (Vec3d pathElm : pathFinderPath) {
            if (i == 0 || i == pathFinderPath.size() - 1) {
                if (lastLoc != null) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                }
                path.add(pathElm.addVector(0.5, 0, 0.5));
                lastDashLoc = pathElm;
            } else {
                boolean canContinue = true;
                if (pathElm.squareDistanceTo(lastDashLoc) > dashDistance * dashDistance) {
                    canContinue = false;
                } else {
                    double smallX = Math.min(lastDashLoc.x, pathElm.x);
                    double smallY = Math.min(lastDashLoc.y, pathElm.y);
                    double smallZ = Math.min(lastDashLoc.z, pathElm.z);
                    double bigX = Math.max(lastDashLoc.x, pathElm.x);
                    double bigY = Math.max(lastDashLoc.y, pathElm.y);
                    double bigZ = Math.max(lastDashLoc.z, pathElm.z);
                    cordsLoop:
                    for (int x = (int) smallX; x <= bigX; x++) {
                        for (int y = (int) smallY; y <= bigY; y++) {
                            for (int z = (int) smallZ; z <= bigZ; z++) {
                                if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                                    canContinue = false;
                                    break cordsLoop;
                                }
                            }
                        }
                    }
                }
                if (!canContinue) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }
            lastLoc = pathElm;
            i++;
        }

        return path;
    }

    public static List<Vector3d> findBlinkPath(final double tpX, final double tpY, final double tpZ) {
        final List<Vector3d> positions = new ArrayList<>();

        double curX = getMc().player.posX;
        double curY = getMc().player.posY;
        double curZ = getMc().player.posZ;
        double distance = Math.abs(curX - tpX) + Math.abs(curY - tpY) + Math.abs(curZ - tpZ);

        for (int count = 0; distance > 0.0D; count++) {
            distance = Math.abs(curX - tpX) + Math.abs(curY - tpY) + Math.abs(curZ - tpZ);

            final double diffX = curX - tpX;
            final double diffY = curY - tpY;
            final double diffZ = curZ - tpZ;
            final double offset = (count & 1) == 0 ? 0.4D : 0.1D;

            final double minX = Math.min(Math.abs(diffX), offset);
            if (diffX < 0.0D) curX += minX;
            if (diffX > 0.0D) curX -= minX;

            final double minY = Math.min(Math.abs(diffY), 0.25D);
            if (diffY < 0.0D) curY += minY;
            if (diffY > 0.0D) curY -= minY;

            double minZ = Math.min(Math.abs(diffZ), offset);
            if (diffZ < 0.0D) curZ += minZ;
            if (diffZ > 0.0D) curZ -= minZ;

            positions.add(new Vector3d(curX, curY, curZ));
        }

        return positions;
    }

    public static List<Vector3d> findPath(final double tpX, final double tpY, final double tpZ, final double offset) {
        final List<Vector3d> positions = new ArrayList<>();
        final double steps = Math.ceil(getDistance(getMc().player.posX, getMc().player.posY, getMc().player.posZ, tpX, tpY, tpZ) / offset);

        final double dX = tpX - getMc().player.posX;
        final double dY = tpY - getMc().player.posY;
        final double dZ = tpZ - getMc().player.posZ;

        for(double d = 1D; d <= steps; ++d) {
            positions.add(new Vector3d(getMc().player.posX + (dX * d) / steps, getMc().player.posY + (dY * d) / steps, getMc().player.posZ + (dZ * d) / steps));
        }

        return positions;
    }

    private static double getDistance(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double xDiff = x1 - x2;
        final double yDiff = y1 - y2;
        final double zDiff = z1 - z2;

        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }
}
