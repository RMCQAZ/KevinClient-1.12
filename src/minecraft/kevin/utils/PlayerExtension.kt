package kevin.utils

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun Entity.getDistanceToEntityBox(entity: Entity): Double {
    val eyes = this.getPositionEyes(1F)
    val pos = getNearestPointBB(eyes, entity.entityBoundingBox)
    val xDist = abs(pos.x - eyes.x)
    val yDist = abs(pos.y - eyes.y)
    val zDist = abs(pos.z - eyes.z)
    return sqrt(xDist.pow(2) + yDist.pow(2) + zDist.pow(2))
}

fun getNearestPointBB(eye: Vec3d, box: AxisAlignedBB): Vec3d {
    val origin = doubleArrayOf(eye.x, eye.y, eye.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)
    for (i in 0..2) {
        if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i]
    }
    return Vec3d(origin[0], origin[1], origin[2])
}

fun EntityPlayer.getPing(): Int {
    val playerInfo = Minecraft.getMinecraft().connection!!.getPlayerInfo(uniqueID)
    return playerInfo.responseTime
}

fun Entity.isAnimal(): Boolean {
    return this is EntityAnimal ||
            this is EntityVillager ||
            this is EntitySquid ||
            this is EntityGolem ||
            this is EntityBat
}

fun Entity.isMob(): Boolean {
    return this is EntityMob ||
            this is EntitySlime ||
            this is EntityGhast ||
            this is EntityDragon
}

fun EntityPlayer.isClientFriend(): Boolean {
    val entityName = name ?: return false

    //return LiquidBounce.fileManager.friendsConfig.isFriend(stripColor(entityName))
    return false
}