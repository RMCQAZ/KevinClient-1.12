package kevin.module.modules.combat

import kevin.event.*
import kevin.font.FontManager
import kevin.module.*
import kevin.utils.*
import kevin.utils.Rotation
import kevin.utils.rainbow.RainbowFontShader
import kevin.utils.timers.MSTimer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.item.*
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Explosion
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.stream.Collectors
import kotlin.math.*

object CrystalAura : Module("CrystalAura","Automatically place and/or attacks crystals", category = ModuleCategory.COMBAT) {

    //Impact
    private var explode = ListValue("Hit", arrayOf("Disable", "Normal", "Packet"), "Packet")
    private val swingMode = ListValue("Swing", arrayOf("Disable", "Swing", "Packet"), "Swing")
    private var waitTick = IntegerValue("TickDelay",1,0,20)
    private var range = FloatValue("HitRange",5F,0F,10F)
    private var walls = FloatValue("WallsRange",3.5F,0F,10F)
    private var antiWeakness = BooleanValue("AntiWeakness",true)
    private var nodesync = BooleanValue("AntiDesync",true)
    private val ignoreNoTarget = BooleanValue("IgnoreNoTarget",false)
    private var place = BooleanValue("Place",true)
    private var autoSwitch = BooleanValue("AutoSwitch",true)
    private var placeRange = FloatValue("PlaceRange",5F,0F,10F)
    private var minDmg = FloatValue("MinDamage",5F,0F,40F)
    private var facePlace = FloatValue("FacePlaceHP",6F,0F,40F)
    private var raytrace = BooleanValue("Raytrace",false)
    private var rotate = BooleanValue("Rotate",true)
    //private var spoofRotations = BooleanValue("SpoofAngles",true)
    private var rainbow = BooleanValue("EspRainbow",true)
    private var espR = IntegerValue("EspRed",200,0,255)
    private var espG = IntegerValue("EspGreen",50,0,255)
    private var espB = IntegerValue("EspBlue",200,0,255)
    private var espA = IntegerValue("EspAlpha",50,0,255)
    private var maxSelfDmg = FloatValue("MaxSelfDmg",10F,0F,36F)
    private var noGappleSwitch = BooleanValue("NoGapSwitch",false)
    private var renderMode = ListValue("EspRenderMode", arrayOf("Box","OtherBox","2D"),"Box")
    private val info = BooleanValue("Info", true)

    private var render: BlockPos? = null
    private var renderEnt: Entity? = null
    private var switchCooldown = false
    private var isAttacking = false
    private var oldSlot = -1
    private var isActive = false
    private var newSlot = 0
    private var waitCounter = 0
    private var f: EnumFacing? = null

    private var damage = .0
    private var selfDamage = 0F
    private val timer = MSTimer()

    //private var isSpoofingAngles = false
    //private var yaw = 0.0
    //private var pitch = 0.0

    override fun onEnable() {
        isActive = false
    }

    override fun onDisable() {
        render = null
        renderEnt = null
        //resetRotation()
        isActive = false
    }

    private fun swingHand(enumHand: EnumHand) {
        when(swingMode.get()) {
            "Swing" -> mc.player.swingArm(enumHand)
            "Packet" -> mc.player.connection.sendPacket(CPacketAnimation(enumHand))
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        isActive = false
        if (mc.player == null || mc.player.isDead) return
        val crystal = mc.world.loadedEntityList.filterIsInstance<EntityEnderCrystal>()
            .filter { e ->
                mc.player.getDistanceToEntity(e) <= range.get()
            }.minByOrNull{ c: EntityEnderCrystal ->
                mc.player.getDistanceToEntity(c)
            }
        if (!(explode equal "Disable") && crystal != null && (renderEnt != null || ignoreNoTarget.get())) {
            if (!mc.player.canEntityBeSeen(crystal) && mc.player.getDistanceToEntity(crystal) > walls.get()) return
            if (waitTick.get() > 0.0) {
                if (waitCounter < waitTick.get()) {
                    waitCounter++
                    return
                }
                waitCounter = 0
            }
            if (antiWeakness.get() && mc.player.isPotionActive(MobEffects.WEAKNESS)) {
                if (!isAttacking) {
                    oldSlot = mc.player.inventory.currentItem
                    isAttacking = true
                }
                newSlot = -1
                for (i in 0..8) {
                    val stack = mc.player.inventory.getStackInSlot(i)
                    if (stack != ItemStack.EMPTY) {
                        if (stack.item is ItemSword) {
                            newSlot = i
                            break
                        }
                        if (stack.item is ItemTool) {
                            newSlot = i
                            break
                        }
                    }
                }
                if (newSlot != -1) {
                    mc.player.inventory.currentItem = newSlot
                    switchCooldown = true
                }
            }
            isActive = true
            if (rotate.get()) lookAtPacket(
                crystal.posX,
                crystal.posY,
                crystal.posZ,
                mc.player
            )

            if (explode equal "Normal")
                mc.playerController.attackEntity(mc.player, crystal)
            else
                mc.player.connection.sendPacket(CPacketUseEntity(crystal))

            swingHand(EnumHand.MAIN_HAND)

            isActive = false
            return
        }
        //resetRotation()
        if (oldSlot != -1) {
            mc.player.inventory.currentItem = oldSlot
            oldSlot = -1
        }
        isAttacking = false
        isActive = false
        var crystalSlot =
            if (mc.player.heldItemMainhand.item === Items.END_CRYSTAL) mc.player.inventory.currentItem else -1
        if (crystalSlot == -1) for (l in 0..8) {
            if (mc.player.inventory.getStackInSlot(l).item === Items.END_CRYSTAL) {
                crystalSlot = l
                break
            }
        }
        var offhand = false
        if (mc.player.heldItemOffhand.item === Items.END_CRYSTAL) {
            offhand = true
        } else if (crystalSlot == -1) {
            return
        }
        val blocks: List<BlockPos> = findCrystalBlocks()
        val entities = ArrayList<Entity?>()
        entities.addAll(
            /**
            mc.world.playerEntities.filter { entityPlayer: EntityPlayer ->
                !entityPlayer.isClientFriend()
            }.sortedBy { e: EntityPlayer ->
                mc.player.getDistanceToEntity(e)
            }
            **/
            mc.world.loadedEntityList.filter { entity: Entity ->
                EntityUtils.isSelected(entity,true)
            }.sortedBy { e: Entity ->
                mc.player.getDistanceToEntity(e)
            }
        )
        var q: BlockPos? = null
        var damage = 0.5
        for (entity in entities) {
            if (entity === mc.player || (entity as EntityLivingBase).health <= 0.0f || entity.isDead || mc.player == null) continue
            for (blockPos in blocks) {
                val b: Double = entity.getDistanceSq(blockPos)
                if (b >= 169.0) continue
                val d = calculateDamage(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5, entity)
                if (d < minDmg.get() && entity.health + entity.absorptionAmount > facePlace.get()) continue
                if (d > damage) {
                    val self =
                        calculateDamage(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5, mc.player)
                    if (self > d && d >= entity.health || self - 0.5 > mc.player.health) continue
                    if (self > maxSelfDmg.get()) continue
                    selfDamage = self
                    this.damage = d.toDouble()
                    damage = d.toDouble()
                    q = blockPos
                    renderEnt = entity
                }
            }
        }
        if (damage == 0.5) {
            render = null
            renderEnt = null
            //resetRotation()
            return
        }
        render = q
        if (place.get()) {
            if (mc.player == null) return
            isActive = true
            if (rotate.get()) lookAtPacket(q!!.x + 0.5, q!!.y - 0.5, q!!.z + 0.5, mc.player as EntityPlayer)
            val result = mc.world.rayTraceBlocks(
                Vec3d(mc.player.posX, mc.player.posY + mc.player.eyeHeight, mc.player.posZ), Vec3d(
                    q!!.x + 0.5, q!!.y - 0.5, q!!.z + 0.5
                )
            )
            if (raytrace.get()) {
                if (result?.sideHit == null) {
                    q = null
                    f = null
                    render = null
                    //resetRotation()
                    isActive = false
                    return
                }
                f = result.sideHit
            }
            val slot = mc.player.inventory.currentItem
            if (!offhand && mc.player.inventory.currentItem != crystalSlot) {
                if (autoSwitch.get()) {
                    if (noGappleSwitch.get() && isEatingGap()) {
                        isActive = false
                        //resetRotation()
                        return
                    }
                    isActive = true
                    mc.player.inventory.currentItem = crystalSlot
                    mc.player.connection.sendPacket(CPacketHeldItemChange(crystalSlot))
                    //resetRotation()
                    //switchCooldown = true
                } else return
                //return
            }
            if (switchCooldown) {
                switchCooldown = false
                //return
            }
            if (mc.player != null) {
                isActive = true
                if (raytrace.get() && f != null) {
                    mc.player.connection.sendPacket(
                        CPacketPlayerTryUseItemOnBlock(
                            q,
                            f,
                            if (offhand) EnumHand.OFF_HAND else EnumHand.MAIN_HAND,
                            0.0f, 0.0f, 0.0f
                        )
                    )
                } else {
                    mc.player.connection.sendPacket(
                        CPacketPlayerTryUseItemOnBlock(
                            q,
                            if (q.y == 255) EnumFacing.DOWN else EnumFacing.UP,
                            if (offhand) EnumHand.OFF_HAND else EnumHand.MAIN_HAND,
                            0.0f,
                            0.0f,
                            0.0f
                        )
                    )
                }
            }
            if (mc.player.inventory.currentItem != slot){
                mc.player.inventory.currentItem = slot
                mc.player.connection.sendPacket(CPacketHeldItemChange(slot))
            }
            isActive = false
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet
        /**
        if (packet is CPacketPlayer && spoofRotations.get() && isSpoofingAngles) {
            packet.yaw = yaw.toFloat()
            packet.pitch = pitch.toFloat()
        }
        **/
        if (packet is SPacketSpawnObject &&
            explode equal "Packet" &&
            packet.type == 51 &&
            (waitTick.get() == 0 || waitCounter >= waitTick.get()) &&
            (renderEnt != null || ignoreNoTarget.get())) {
            val crystal = EntityEnderCrystal(mc.world, packet.x, packet.y, packet.z)
            if (!mc.player.canEntityBeSeen(crystal) && mc.player.getDistanceToEntity(crystal) > walls.get()) return
            if (mc.player.getDistanceToEntity(crystal) > range.get()) return
            mc.player.connection.sendPacket(CPacketUseEntity(packet.entityID))
            swingHand(EnumHand.MAIN_HAND)
        }
        if (packet is SPacketSoundEffect && nodesync.get()) {
            if (packet.category == SoundCategory.BLOCKS && packet.sound === SoundEvents.ENTITY_GENERIC_EXPLODE) for (e in Minecraft.getMinecraft().world.loadedEntityList) {
                if (e is EntityEnderCrystal && e.getDistance(packet.x, packet.y, packet.z) <= 6.0) e.setDead()
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent){
        if (render != null && mc.player != null) {
            if (rainbow.get()) {
                drawCurrentBlock(render!!, ColorUtils.rainbow(espA.get()))
            } else {
                drawCurrentBlock(render!!, Color(espR.get(), espG.get(), espB.get(), espA.get()))
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (info.get()) {
            GL11.glPushMatrix()
            if (render!=null) timer.reset()
            val damageStr = "Damage: ${if (timer.hasTimePassed(1500)) "--" else damage}"
            val selfDmgStr = "Self: ${if (timer.hasTimePassed(1500)) "--" else selfDamage}"
            val posStr = "Pos: ${render ?: "No Target"}"
            val scaledResolution = ScaledResolution(mc)

            GlStateManager.resetColor()

            val s = 0.7
            val color = Color.white.rgb
            GL11.glScaled(s, s, s)
            RainbowFontShader.begin(true, 1.0F / 1000, 1.0F / 1000 ,System.currentTimeMillis() % 10000 / 10000F).use {
                FontManager.font35.drawString(
                    damageStr, ((scaledResolution.scaledWidth / 2F + 10F) / s).toFloat(),
                    ((scaledResolution.scaledHeight / 2 - 7F) / s).toFloat(), color, true
                )
                FontManager.font35.drawString(
                    selfDmgStr, ((scaledResolution.scaledWidth / 2F + 10F) / s).toFloat(),
                    ((scaledResolution.scaledHeight / 2 - 7F - (FontManager.font35.fontHeight * s)) / s).toFloat(), color, true
                )
                FontManager.font35.drawString(
                    posStr, ((scaledResolution.scaledWidth / 2F + 10F) / s).toFloat(),
                    ((scaledResolution.scaledHeight / 2 - 7F - (FontManager.font35.fontHeight * 2F * s)) / s).toFloat(), color, true
                )
            }

            GlStateManager.enableBlend()
            GL11.glPopMatrix()
        }
    }

    private fun isEatingGap(): Boolean {
        return mc.player.heldItemMainhand.item is ItemAppleGold && mc.player.isHandActive
    }

    private fun drawCurrentBlock(render: BlockPos, color: Color) {
        when(renderMode.get()) {
            "Box", "OtherBox" -> RenderUtils.drawBlockBox(render, color, renderMode equal "Box")
            "2D" -> RenderUtils.draw2D(render, color.rgb, Color.BLACK.rgb)
        }
    }

    private fun lookAtPacket(px: Double, py: Double, pz: Double, me: EntityPlayer) {
        val v = calculateLookAt(px, py, pz, me)
        //setYawAndPitch(v[0].toFloat(), v[1].toFloat())
        RotationUtils.setTargetRotation(Rotation(v[0].toFloat(), v[1].toFloat()))
    }

    fun canPlaceCrystal(blockPos: BlockPos): Boolean {
        val boost = blockPos.add(0, 1, 0)
        val boost2 = blockPos.add(0, 2, 0)
        return (mc.world.getBlockState(blockPos).block === Blocks.BEDROCK || mc.world.getBlockState(blockPos).block === Blocks.OBSIDIAN) && mc.world.getBlockState(
            boost
        ).block === Blocks.AIR && mc.world.getBlockState(boost2).block === Blocks.AIR && mc.world.getEntitiesWithinAABB(
            Entity::class.java, AxisAlignedBB(boost)
        ).isEmpty() && mc.world.getEntitiesWithinAABB(
            Entity::class.java, AxisAlignedBB(boost2)
        ).isEmpty()
    }

    private fun getPlayerPos(): BlockPos {
        return BlockPos(floor(mc.player.posX), floor(mc.player.posY), floor(mc.player.posZ))
    }

    private fun findCrystalBlocks(): List<BlockPos> {
        val positions = NonNullList.create<BlockPos>()
        positions.addAll(
            getSphere(
                getPlayerPos(),
                placeRange.get(),
                placeRange.get().toInt(),
                false,
                true,
                0
            ).stream().filter { blockPos: BlockPos ->
                canPlaceCrystal(blockPos)
            }.collect(Collectors.toList())
        )
        return positions as List<BlockPos>
    }

    private fun getSphere(loc: BlockPos, r: Float, h: Int, hollow: Boolean, sphere: Boolean, plus_y: Int): List<BlockPos> {
        val circleblocks: MutableList<BlockPos> = ArrayList()
        val cx = loc.x
        val cy = loc.y
        val cz = loc.z
        for (x in (cx-r).toInt()..(cx+r).toInt()){
            for (z in (cz-r).toInt()..(cz+r).toInt()){
                for (y in (if (sphere) cy - r.toInt() else cy)..(if (sphere) (cy + r) else (cy + h)).toInt()){
                    val dist = ((cx - x) * (cx - x) + (cz - z) * (cz - z) + if (sphere) (cy - y) * (cy - y) else 0).toDouble()
                    if (dist < r * r && (!hollow || dist >= (r - 1.0f) * (r - 1.0f))) {
                        val l = BlockPos(x, y + plus_y, z)
                        circleblocks.add(l)
                    }
                }
            }
        }
        /**
        var x = cx - r.toInt()
        while (x <= cx + r) {
        var z = cz - r.toInt()
        while (z <= cz + r) {
        var y = if (sphere) cy - r.toInt() else cy
        while (true) {
        if (y < (if (sphere) (cy + r) else (cy + h)).toInt()) {
        val dist = ((cx - x) * (cx - x) + (cz - z) * (cz - z) + if (sphere) (cy - y) * (cy - y) else 0).toDouble()
        if (dist < r * r && (!hollow || dist >= (r - 1.0f) * (r - 1.0f))) {
        val l = BlockPos(x, y + plus_y, z)
        circleblocks.add(l)
        }
        y++
        z++
        continue
        }
        z++
        }
        }
        x++
        }
         **/
        return circleblocks
    }

    private fun calculateDamage(posX: Double, posY: Double, posZ: Double, entity: Entity): Float {
        val doubleExplosionSize = 12.0f
        val distancedsize = entity.getDistance(posX, posY, posZ) / doubleExplosionSize
        val vec3d = Vec3d(posX, posY, posZ)
        val blockDensity = entity.world.getBlockDensity(vec3d, entity.entityBoundingBox).toDouble()
        val v = (1.0 - distancedsize) * blockDensity
        val damage = ((v * v + v) / 2.0 * 7.0 * doubleExplosionSize + 1.0).toInt().toFloat()
        var finald = 1.0
        if (entity is EntityLivingBase) finald = getBlastReduction(
            entity,
            getDamageMultiplied(damage),
            Explosion(mc.world, null, posX, posY, posZ, 6.0f, false, true)
        ).toDouble()
        return finald.toFloat()
    }

    private fun getBlastReduction(entity: EntityLivingBase, damage: Float, explosion: Explosion?): Float {
        var damage = damage
        if (entity is EntityPlayer) {
            val ep = entity
            val ds = DamageSource.causeExplosionDamage(explosion)
            damage = CombatRules.getDamageAfterAbsorb(
                damage,
                ep.totalArmorValue.toFloat(),
                ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue.toFloat()
            )
            val k = EnchantmentHelper.getEnchantmentModifierDamage(ep.armorInventoryList, ds)
            val f = MathHelper.clamp(k.toFloat(), 0.0f, 20.0f)
            damage *= 1.0f - f / 25.0f
            if (entity.isPotionActive(Potion.getPotionById(11))) damage -= damage / 4.0f
            return damage
        }
        damage = CombatRules.getDamageAfterAbsorb(
            damage,
            entity.totalArmorValue.toFloat(),
            entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue.toFloat()
        )
        return damage
    }

    private fun getDamageMultiplied(damage: Float): Float {
        val diff: Int = mc.world.difficulty.difficultyId
        return damage * if (diff == 0) 0.0f else if (diff == 2) 1.0f else if (diff == 1) 0.5f else 1.5f
    }

    /**
    private fun setYawAndPitch(yaw1: Float, pitch1: Float) {
        yaw = yaw1.toDouble()
        pitch = pitch1.toDouble()
        isSpoofingAngles = true
    }

    private fun resetRotation() {
        if (isSpoofingAngles) {
            yaw = mc.player.rotationYaw.toDouble()
            pitch = mc.player.rotationPitch.toDouble()
            isSpoofingAngles = false
        }
    }
**/
    private fun calculateLookAt(px: Double, py: Double, pz: Double, me: EntityPlayer): DoubleArray {
        var dirx = me.posX - px
        var diry = me.posY - py
        var dirz = me.posZ - pz
        val len = sqrt(dirx * dirx + diry * diry + dirz * dirz)
        dirx /= len
        diry /= len
        dirz /= len
        var pitch = Math.asin(diry)
        var yaw = Math.atan2(dirz, dirx)
        pitch = pitch * 180.0 / Math.PI
        yaw = yaw * 180.0 / Math.PI
        yaw += 90.0
        return doubleArrayOf(yaw, pitch)
    }

    /**
    // General
    private val maxTargets = IntegerValue("Max Targets", 4, 1, 10)
    private val targetRange = FloatValue("Target Range", 12.0f, 0.0f ,24.0f)
    private val rotation = BooleanValue("Rotation", true)
    private val yawSpeed = FloatValue("Yaw Speed", 45.0f, 5.0f, 180.0f)
    private val placeRotationRange = FloatValue("Place Rotation Range", 0.0f, 0.0f, 180.0f)
    private val breakRotationRange = FloatValue("Break Rotation Range", 90.0f, 0.0f, 180.0f)
    private val eatingPause = BooleanValue("Eating Pause", false)
    private val miningPause = BooleanValue("Mining Pause", false)

    // Force Place
    private val forcePlaceHealth = FloatValue("Force Place Health", 8.0f, 0.0f, 20.0f)
    private val forcePlaceArmorRate = IntegerValue("Force Place Armor Rate", 3, 0, 25)
    private val forcePlaceMinDamage = FloatValue("Force Place Min Damage", 1.5f, 0.0f, 10.0f)
    private val forcePlaceMotion = FloatValue("Force Place Motion", 4.0f, 0.0f, 10.0f)
    private val forcePlaceBalance = FloatValue("Force Place Balance", -1.0f, -10.0f, 10.0f)
    private val forcePlaceSword = BooleanValue("Force Place Sword", false)

    // Calculation
    private val noSuicide = FloatValue("No Suicide", 2.0f, 0.0f, 20.0f)
    private val wallRange = FloatValue("Wall Range", 3.0f, 0.0f, 8.0f)
    private val forceUpdate = BooleanValue("Force Update", true)
    private val updateDelay = IntegerValue("Update Delay", 25, 0, 250)
    private val motionPredict = BooleanValue("Motion Predict", true)
    private val predictTicks = IntegerValue("Predict Ticks", 8, 0, 20)
    private val breakCalculation = ListValue("Break Calculation", arrayOf("Single", "Multi"), "Multi")
    private val lethalOverride = BooleanValue("Lethal Override", true)
    private val lethalBalance = FloatValue("Lethal Balance", 0.5f, -5.0f, 5.0f)
    private val lethalMaxDamage = FloatValue("Lethal Max Damage", 16.0f, 0.0f, 20.0f)
    private val safeOverride = BooleanValue("Safe Override", true)
    private val safeRange = FloatValue("Safe Range", 0.5f, 0.0f, 5.0f)
    private val safeThreshold = FloatValue("Safe Threshold", 2.0f, 0.0f, 5.0f)

    // Place
    private val placeMode = ListValue("Place Mode", arrayOf("Off", "Single", "Multi"), "Single")
    private val packetPlace = ListValue("Packet Place", arrayOf("Off", "Weak", "Strong"), "Weak")
    private val spamPlace = BooleanValue("Spam Place", true)
    private val autoSwap = ListValue("Auto Swap", arrayOf("Off", "Swap", "Spoof"), "Off")
    private val newPlacement = BooleanValue("1.13 Place", false)
    private val placeSwing = BooleanValue("Place Swing", false)
    private val placeBypass = ListValue("Place Bypass", arrayOf("Up", "Down", "Closest"), "Up")
    private val placeMinDamage = FloatValue("Place Min Damage", 5.0f, 0.0f, 20.0f)
    private val placeMaxSelfDamage = FloatValue("Place Max Self Damage", 6.0f, 0.0f, 20.0f)
    private val placeBalance = FloatValue("Place Balance", -3.0f, -10.0f, 10.0f)
    private val placeDelay = IntegerValue("Place Delay", 50, 0, 500)
    private val placeRange = FloatValue("Place Range", 5.0f, 0.0f, 8.0f)

    // Break
    private val breakMode = ListValue("Break Mode", arrayOf("Off", "Target", "Own", "Smart", "All"), "Smart")
    private val bbtt = BooleanValue("2B2T", false)
    private val bbttFactor = IntegerValue("2B2T Factor", 200, 0, 1000)
    private val packetBreak = ListValue("Packet Break", arrayOf("Off", "Target", "Own", "Smart", "All"), "Target")
    private val ownTimeout = IntegerValue("Own Timeout", 100, 0, 2000)
    private val antiWeakness = ListValue("Anti Weakness", arrayOf("Off", "Swap", "Spoof"), "Off")
    private val swapDelay = IntegerValue("Swap Delay", 0, 0, 20)
    private val breakMinDamage = FloatValue("Break Min Damage", 4.0f, 0.0f, 20.0f)
    private val breakMaxSelfDamage = FloatValue("Break Max Self Damage", 8.0f, 0.0f, 20.0f)
    private val breakBalance = FloatValue("Break Balance", -4.0f, -10.0f, 10.0f)
    private val breakDelay = IntegerValue("Break Delay", 100, 0, 500)
    private val breakRange = FloatValue("Break Range", 5.0f, 0.0f, 8.0f)

    // Misc
    private val swingMode = ListValue("Swing Mode", arrayOf("Client", "Packet"), "Client")
    private val swingHand = ListValue("Swing Hand", arrayOf("Auto", "Off Hand", "Main Hand"), "Auto")
    private val filledAlpha = IntegerValue("Filled Alpha", 63, 0, 255)
    private val outlineAlpha = IntegerValue("Outline Alpha", 200, 0, 255)
    private val targetDamage = BooleanValue("Target Damage", true)
    private val selfDamage = BooleanValue("Self Damage", true)
    private val targetChams = ListValue("Target Chams", arrayOf("Off", "Single", "Multi"), "Single")
    private val chamsAlpha = IntegerValue("Chams Alpha", 64, 0, 255)
    private val renderPredict = ListValue("Render Predict", arrayOf("Off", "Single", "Multi"), "Single")
    private val hudInfo = ListValue("Hud Info", arrayOf("Off", "Speed", "Target", "Damage"), "Speed")
    private val movingLength = IntegerValue("Moving Length", 400, 0, 1000)
    private val fadeLength = IntegerValue("Fade Length", 200, 0, 1000)

    private fun swingHand(hand: EnumHand){
        if (swingMode equal "Client")
            mc.player.swingArm(hand)
        else
            mc.player.connection.sendPacket(CPacketAnimation(hand))
    }
    private val packetPlaceOnRemove: Boolean
    get() = packetPlace.get() != "Off"
    private val packetPlaceOnBreak: Boolean
    get() = packetPlace.get() == "Strong"

    private val renderTargetSet: IntOpenHashSet
    get() = IntOpenHashSet().apply {
        targets.forEach {
            add(it.entity.entityId)
        }
    }
    private val targets: Sequence<TargetInfo>
    get() = getTargetEntities()
    private val rotationInfo: PlaceInfo?
    get() = calcPlaceInfo(false)
    private val placeInfo: PlaceInfo?
    get() = calcPlaceInfo(rotation.get())
    private val renderPlaceInfo: PlaceInfo?
    get() = if (rotation.get()) rotationInfo else placeInfo

    @JvmStatic
    val target: EntityLivingBase?
        get() = placeInfo?.target

    private val placedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val crystalSpawnMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val attackedCrystalMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val timeoutTimer = TickTimer()
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private var lastActiveTime = 0L
    private var lastRotation: PlaceInfo? = null
    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray<Int>(8)
    private var explosionCount = 0

    private val reductionMap = Collections.synchronizedMap(WeakHashMap<EntityLivingBase, DamageReduction>())

    override val tag: String?
        get() = when (hudInfo.get()) {
            "Speed" -> "%.1f".format(explosionCountArray.average() * 4.0)
            "Damage" -> renderPlaceInfo?.let { "%.1f/%.1f".format(it.targetDamage, it.selfDamage) } ?: "0.0/0.0"
            "Target" -> target?.name ?: "None"
            else -> null
        }
    override fun onEnable() {

    }
    override fun onDisable() {
        lastActiveTime = 0L
        lastRotation = null
        explosionTimer.reset(-114514L)
        explosionCountArray.clear()
        explosionCount = 0
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent){
        /**
        if (renderPredict != RenderMode.OFF) {
            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer
            val partialTicks = RenderUtils3D.partialTicks
            GlStateManager.color(0.3f, 1.0f, 0.3f, 1.0f)
            GlStateManager.glLineWidth(2.0f)
            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION)
            buffer.setTranslation(-mc.renderManager.renderPosX, -mc.renderManager.renderPosY, -mc.renderManager.renderPosZ)
            if (renderPredict == RenderMode.SINGLE) {
                val placeInfo = renderPlaceInfo
                if (placeInfo != null) {
                    targets.getLazy()?.find {
                        it.entity == placeInfo.target
                    }?.let {
                        drawEntityPrediction(buffer, it.entity, it.predictMotion, partialTicks)
                    }
                } else {
                    targets
                }
            } else {
                targets.getLazy()?.forEach {
                    drawEntityPrediction(buffer, it.entity, it.predictMotion, partialTicks)
                }
            }
            GlStateUtils.useProgram(0)
            tessellator.draw()
            buffer.setTranslation(0.0, 0.0, 0.0)
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateManager.glLineWidth(1.0f)
        }
        Renderer.onRender3D()
        **/
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent){
        //Renderer.onRender2D()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        for (entity in mc.world.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            reductionMap[entity] = DamageReduction(entity)
        }
        if (explosionTimer.tickAndReset(250L)) {
            val count = explosionCount
            explosionCount = 0
            explosionCountArray.add(count)
        }
        runLoop()
    }

    private val crystalList: List<EntityEnderCrystal>
    get() = mc.world.loadedEntityList.filterIsInstance<EntityEnderCrystal>().toList()

    @EventTarget
    fun onPacket(event: PacketEvent){
        if (event.packet is SPacketSpawnObject) {
            handleSpawnObject(event.packet)
        }
        if (event.packet is SPacketSoundEffect){
            if (event.packet.category == SoundCategory.BLOCKS && event.packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                val list = crystalList.asSequence()
                    .filter { it.getDistanceSq(event.packet.x, event.packet.y, event.packet.z) <= 144.0 }
                    .toList()

                if (list.isNotEmpty()) {
                    handlePlacedExplosion(event.packet.x, event.packet.y, event.packet.z, list)
                    placedPosMap.clear()
                    crystalSpawnMap.clear()
                    attackedCrystalMap.clear()
                }
            }
        }
    }
    private val NUM_X_BITS = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000))
    private val NUM_Z_BITS = NUM_X_BITS
    private val NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS
    private val Y_SHIFT = 0 + NUM_Z_BITS
    private val X_SHIFT = Y_SHIFT + NUM_Y_BITS
    private val X_MASK = (1L shl NUM_X_BITS) - 1L
    private val Y_MASK = (1L shl NUM_Y_BITS) - 1L
    private val Z_MASK = (1L shl NUM_Z_BITS) - 1L
    private inline fun toLong(x: Int, y: Int, z: Int): Long {
        return x.toLong() and X_MASK shl X_SHIFT or
                (y.toLong() and Y_MASK shl Y_SHIFT) or
                (z.toLong() and Z_MASK)
    }
    private inline fun toLong(x: Double, y: Double, z: Double): Long {
        return toLong(x.fastFloor(), y.fastFloor(), z.fastFloor())
    }
    private inline fun handlePlacedExplosion(posX: Double, posY: Double, posZ: Double, list: List<EntityEnderCrystal>) {
        placeInfo?.let {
            for (crystal in list) {
                if (placeBoxIntersectsCrystalBox(crystal.posX, crystal.posY, crystal.posZ, it.blockPos)) {
                    if (packetPlaceOnRemove) placeDirect(it)
                    explosionCount++
                    return
                }
            }
        }
        if (placedPosMap.containsKey(toLong(posX, posY - 1.0, posZ))) {
            explosionCount++
        }
    }

    private inline fun checkCrystalRotation(box: AxisAlignedBB, eyePos: Vec3d, sight: Vec3d): Boolean {
        return !rotation.get()
                || box.calculateIntercept(eyePos, sight) != null
                || breakRotationRange.get() != 0.0f && checkRotationDiff(getRotationTo(eyePos, box.center), breakRotationRange.get())
    }
    @Suppress("UNUSED_PARAMETER")
    private inline fun checkCrystalRotation(x: Double, y: Double, z: Double): Boolean {
        if (!rotation.get()) return true
        val eyePos = mc.player.positionVector.addVector(0.0, mc.player.eyeHeight.toDouble(), 0.0)
        val sight = eyePos.add(RotationUtils.serverRotation.toViewVec().scale(8.0))
        return checkCrystalRotation(getCrystalBB(x, y, z), eyePos, sight)
    }
    private fun getCrystalBB(x: Double, y: Double, z: Double): AxisAlignedBB {
        return AxisAlignedBB(
            x - 1.0, y, z - 1.0,
            x + 1.0, y + 2.0, z + 1.0
        )
    }
    private inline fun runLoop() {
        val breakFlag = breakMode.get() != "Off" && breakTimer.tick(breakDelay.get())
        val placeFlag = placeMode.get() != "Off" && placeTimer.tick(placeDelay.get())
        if (timeoutTimer.tickAndReset(5L)) {
            updateTimeouts()
        }
        if (breakFlag || placeFlag) {
            mc.playerController.syncCurrentPlayItem()
            val placeInfo = if (forceUpdate.get()) placeInfo else placeInfo
            placeInfo?.let {
                if (checkPausing()) return
                if (breakFlag) doBreak(placeInfo)
                if (placeFlag) doPlace(placeInfo)
            }
        }
    }
    private inline fun doPlace(placeInfo: PlaceInfo) {
        if (spamPlace.get() || checkPlaceCollision(placeInfo)) {
            placeDirect(placeInfo)
        }
    }
    private inline fun swingHand() {
        val hand = when (swingHand.get()) {
            "Off Hand" -> EnumHand.OFF_HAND
            "Main Hand" -> EnumHand.MAIN_HAND
            else -> if (mc.player.heldItemOffhand.item == Items.END_CRYSTAL) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
        }
        swingHand(hand)
    }
    private inline fun EntityPlayerSP.isWeaknessActive(): Boolean {
        return this.isPotionActive(MobEffects.WEAKNESS)
                && this.getActivePotionEffect(MobEffects.STRENGTH)?.let {
            it.amplifier <= 0
        } ?: true
    }
    private inline fun isHoldingTool(): Boolean {
        val item = mc.player.heldItemMainhand.item
        return item is ItemTool || item is ItemSword
    }
    private inline fun breakDirect(x: Double, y: Double, z: Double, entityID: Int) {
        if (autoSwap.get() != "Spoof" && antiWeakness.get() != "Spoof"/* && System.currentTimeMillis() - HotbarManager.swapTime < swapDelay.get() * 50L*/) return
        if (mc.player.isWeaknessActive() && !isHoldingTool()) {
            when (antiWeakness.get()) {
                "Swap" -> {
                    //val slot = getWeaponSlot() ?: return
                    //swapToSlot(slot)
                    if (autoSwap.get() != "Spoof" && swapDelay.get() != 0) return
                    mc.connection!!.sendPacket(CPacketUseEntity(entityID))
                    swingHand()
                }
                "Spoof" -> {
                    //val slot = getWeaponSlot() ?: return
                    val packet = CPacketUseEntity(entityID)
                    /*spoofHotbar(slot) {
                        mc.connection!!.sendPacket(packet)
                        swingHand()
                    }*/
                }
                else -> {
                    return
                }
            }
        } else {
            mc.connection!!.sendPacket(CPacketUseEntity(entityID))
            swingHand()
        }
        placeInfo?.let {
            if (packetPlaceOnBreak && placeBoxIntersectsCrystalBox(x, y, z, it.blockPos)) {
                placeDirect(it)
            }
            mc.player.setLastAttackedEntity(it.target)
        }
        attackedCrystalMap[entityID] = System.currentTimeMillis() + 1000L
        breakTimer.reset()
        lastActiveTime = System.currentTimeMillis()
    }
    private inline fun getHandNullable(): EnumHand? {
        return when (Items.END_CRYSTAL) {
            mc.player.heldItemOffhand.item -> EnumHand.OFF_HAND
            mc.player.heldItemMainhand.item -> EnumHand.MAIN_HAND
            else -> null
        }
    }
    private inline val EntityPlayer.allSlots: List<Slot>
        get() = inventoryContainer.getSlots(1..45)
    private inline fun Container.getSlots(range: IntRange): List<Slot> =
        inventorySlots.subList(range.first, range.last + 1)
    private fun Iterable<Slot>.countItem(item: Item, predicate: Predicate<ItemStack>? = null) =
        countByStack {
            it.item == item && (predicate == null || predicate.test(it))
        }
    private fun Iterable<Slot>.countByStack(predicate: Predicate<ItemStack>? = null) =
        sumOf { slot ->
            slot.stack.let { if (predicate == null || predicate.test(it)) it.count else 0 }
        }
    private inline fun EntityPlayerSP.getCrystalSlot(): Slot? {
        return this.hotbarSlots.firstItem(Items.END_CRYSTAL)
    }
    private inline val EntityPlayer.hotbarSlots: List<Slot>
        get() = ArrayList<Slot>().apply {
            for (slot in 36..44) {
                add(inventoryContainer.inventorySlots[slot])
            }
        }
    private fun <T : Slot> Iterable<T>.firstItem(item: Item, predicate: Predicate<ItemStack>? = null) =
        firstByStack {
            it.item == item && (predicate == null || predicate.test(it))
        }
    private fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? =
        firstOrNull {
            (predicate == null || predicate.test(it.stack))
        }
    private inline fun placeDirect(placeInfo: PlaceInfo) {
        if (mc.player.allSlots.countItem(Items.END_CRYSTAL) == 0) return
        val hand = getHandNullable()
        if (hand == null) {
            when (autoSwap.get()) {
                "Swap" -> {
                    val slot = mc.player.getCrystalSlot() ?: return
                    //swapToSlot(slot)
                    mc.player.connection.sendPacket(placePacket(placeInfo, EnumHand.MAIN_HAND))
                }
                "Spoof" -> {
                    val slot = mc.player.getCrystalSlot() ?: return
                    val packet = placePacket(placeInfo, EnumHand.MAIN_HAND)
                    /*spoofHotbar(slot) {
                        mc.player.connection.sendPacket(packet)
                    }*/
                }
                else -> return
            }
        } else {
            mc.player.connection.sendPacket(placePacket(placeInfo, hand))
        }
        placedPosMap[placeInfo.blockPos.toLong()] = System.currentTimeMillis() + ownTimeout.get()
        if (placeSwing.get()) swingHand()
        placeTimer.reset()

        lastActiveTime = System.currentTimeMillis()
    }
    private inline fun placePacket(placeInfo: PlaceInfo, hand: EnumHand): CPacketPlayerTryUseItemOnBlock {
        return CPacketPlayerTryUseItemOnBlock(placeInfo.blockPos, placeInfo.side, hand, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z)
    }
    private fun placeBoxIntersectsCrystalBox(placeX: Double, placeY: Double, placeZ: Double, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placeY in 0.0..2.0
                && abs(crystalPos.x - placeX) < 2.0
                && abs(crystalPos.z - placeZ) < 2.0
    }
    private inline fun checkPlaceCollision(placeInfo: PlaceInfo): Boolean {
        return mc.world.loadedEntityList.asSequence()
            .filter { it.isEntityAlive }
            .filterIsInstance<EntityEnderCrystal>()
            .filter { placeBoxIntersectsCrystalBox(it.posX, it.posY, it.posZ, placeInfo.blockPos) }
            .filterNot { attackedCrystalMap.containsKey(it.entityId) }
            .none()
    }
    private inline fun getCrystals(): List<EntityEnderCrystal> {
        val eyePos = mc.player.getPositionEyes(1F)
        val sight = eyePos.add(RotationUtils.serverRotation.toViewVec().scale(8.0))
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return mc.world.loadedEntityList.asSequence()
            .filter { it.isEntityAlive }
            .filterIsInstance<EntityEnderCrystal>()
            .runIf(bbtt.get()) {
                val current = System.currentTimeMillis()
                filter { current - getSpawnTime(it) >= bbttFactor.get() }
            }
            .filter { checkBreakRange(it, mutableBlockPos) }
            .filter { checkCrystalRotation(it.entityBoundingBox, eyePos, sight) }
            .toList()
    }
    private inline fun getSpawnTime(crystal: EntityEnderCrystal): Long {
        return crystalSpawnMap.computeIfAbsent(crystal.entityId) {
            System.currentTimeMillis() - crystal.ticksExisted * 50
        }
    }
    private inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
        return if (boolean) block.invoke(this)
        else this
    }
    private inline fun getTargetCrystal(placeInfo: PlaceInfo, crystalList: List<EntityEnderCrystal>): EntityEnderCrystal? {
        return crystalList.firstOrNull {
            placeBoxIntersectsCrystalBox(it.posX, it.posY, it.posZ, placeInfo.blockPos)
        }
    }
    private open class BreakInfo(
        open val crystal: EntityEnderCrystal,
        open val selfDamage: Float,
        open val targetDamage: Float
    ) {
        class Mutable : BreakInfo(DUMMY_CRYSTAL, Float.MAX_VALUE, forcePlaceMinDamage.get()) {
            override var crystal = super.crystal; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set

            fun update(
                target: EntityEnderCrystal,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.crystal = target
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }

            fun clear() {
                update(DUMMY_CRYSTAL, Float.MAX_VALUE, forcePlaceMinDamage.get())
            }
        }

        fun takeValid(): BreakInfo? {
            return this.takeIf {
                crystal !== DUMMY_CRYSTAL
                        && selfDamage != Float.MAX_VALUE
                        && targetDamage != forcePlaceMinDamage.get()
            }
        }

        companion object {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            private val DUMMY_CRYSTAL = EntityEnderCrystal(null, 0.0, 0.0, 0.0)
        }
    }
    @Suppress("DuplicatedCode")
    private inline fun getCrystal(crystalList: List<EntityEnderCrystal>): EntityEnderCrystal? {
        val max = BreakInfo.Mutable()
        val safe = BreakInfo.Mutable()
        val lethal = BreakInfo.Mutable()
        val targets = targets.toList()
        val playerPos = mc.player.positionVector
        val playerBox = mc.player.boundingBox
        val noSuicide = noSuicide
        val mutableBlockPos = BlockPos.MutableBlockPos()
        if (targets.isNotEmpty()) {
            for (crystal in crystalList) {
                val selfDamage = calcDamage(mc.player, playerPos, playerBox, crystal.posX, crystal.posY, crystal.posZ, mutableBlockPos)
                if (mc.player.scaledHealth - selfDamage <= noSuicide.get()) continue
                if (!lethalOverride.get() && selfDamage > breakMaxSelfDamage.get()) continue
                for ((entity, entityPos, entityBox) in targets) {
                    val targetDamage = calcDamage(entity, entityPos, entityBox, crystal.posX, crystal.posY, crystal.posZ, mutableBlockPos)
                    if (lethalOverride.get() && targetDamage - entity.totalHealth > lethalBalance.get() && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxDamage.get()) {
                        lethal.update(crystal, selfDamage, targetDamage)
                    }
                    if (selfDamage > breakMaxSelfDamage.get()) continue
                    val minDamage: Float
                    val balance: Float
                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage.get()
                        balance = forcePlaceBalance.get()
                    } else {
                        minDamage = breakMinDamage.get()
                        balance = breakBalance.get()
                    }
                    if (targetDamage >= minDamage && targetDamage - selfDamage >= balance) {
                        if (targetDamage > max.targetDamage) {
                            max.update(crystal, selfDamage, targetDamage)
                        } else if (max.targetDamage - targetDamage <= safeRange.get()
                            && max.selfDamage - selfDamage >= safeThreshold.get()) {
                            safe.update(crystal, selfDamage, targetDamage)
                        }
                    }
                }
            }
        }
        if (max.targetDamage - safe.targetDamage > safeRange.get()
            || max.selfDamage - safe.selfDamage <= safeThreshold.get()) {
            safe.clear()
        }
        val valid = lethal.takeValid()
            ?: safe.takeValid()
            ?: max.takeValid()

        return valid?.crystal
    }
    private inline fun doBreak(placeInfo: PlaceInfo) {
        val crystalList = getCrystals()
        val crystal = when (breakMode.get()) {
            "Own" -> {
                getTargetCrystal(placeInfo, crystalList)
                    ?: getCrystal(crystalList.filter { placedPosMap.containsKey(toLong(it.posX, it.posY - 1.0, it.posZ)) })
            }
            "Target" -> {
                getTargetCrystal(placeInfo, crystalList)
            }
            "Smart" -> {
                getTargetCrystal(placeInfo, crystalList)
                    ?: getCrystal(crystalList)
            }
            "All" -> {
                val entity = target ?: mc.player
                crystalList.minByOrNull { entity.getDistanceSqToEntity(it) }
            }
            else -> {
                return
            }
        }
        crystal?.let {
            breakDirect(it.posX, it.posY, it.posZ, it.entityId)
        }
    }
    private inline fun updateTimeouts() {
        val current = System.currentTimeMillis()
        synchronized(placedPosMap) {
            placedPosMap.values.removeIf {
                it < current
            }
        }
        synchronized(crystalSpawnMap) {
            crystalSpawnMap.values.removeIf {
                it + 5000L < current
            }
        }
        synchronized(attackedCrystalMap) {
            attackedCrystalMap.values.removeIf {
                it < current
            }
        }
    }
    private inline fun checkPausing(): Boolean {
        return eatingPause.get() && mc.player.isHandActive && mc.player.activeItemStack.item is ItemFood
                || miningPause.get() && mc.playerController.isHittingBlock
    }
    private inline fun handleSpawnObject(packet: SPacketSpawnObject) {
        val mutableBlockPos = BlockPos.MutableBlockPos()
        if (packet.type == 51) {
            if (checkBreakRange(packet.x, packet.y, packet.z, mutableBlockPos)) {
                crystalSpawnMap[packet.entityID] = System.currentTimeMillis()

                if (!bbtt.get() && checkCrystalRotation(packet.x, packet.y, packet.z)) {
                    placeInfo?.let {
                        when (packetBreak.get()) {
                            "Target" -> {
                                if (placeBoxIntersectsCrystalBox(packet.x, packet.y, packet.z, it.blockPos)) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            "Own" -> {
                                if (placeBoxIntersectsCrystalBox(packet.x, packet.y, packet.z, it.blockPos)
                                    || placedPosMap.containsKey(toLong(packet.x, packet.y - 1.0, packet.z))
                                    && checkBreakDamage(packet.x, packet.y, packet.z, mutableBlockPos)) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            "Smart" -> {
                                if (placeBoxIntersectsCrystalBox(packet.x, packet.y, packet.z, it.blockPos)
                                    || checkBreakDamage(packet.x, packet.y, packet.z, mutableBlockPos)) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            "All" -> {
                                breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                            }
                            else -> {
                                return
                            }
                        }
                    }
                }
            }
        }
    }
    private inline fun checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val selfDamage = max(calcDamage(mc.player,mc.player.positionVector,mc.player.boundingBox,crystalX, crystalY, crystalZ, mutableBlockPos), calcDamage(mc.player,mc.player.positionVector,mc.player.boundingBox,crystalX, crystalY, crystalZ, mutableBlockPos))
        if (mc.player.scaledHealth - selfDamage <= noSuicide.get()) return false
        val ticks = if (motionPredict.get()) predictTicks.get() else 0
        return when (breakCalculation.get()) {
            "Single" -> {
                target?.let {
                    checkBreakDamage(crystalX, crystalY, crystalZ, selfDamage, getTargetInfo(it, ticks), mutableBlockPos)
                } ?: false
            }
            else -> {
                targets.any {
                    checkBreakDamage(crystalX, crystalY, crystalZ, selfDamage, it, mutableBlockPos)
                }
            }
        }
    }
    private inline fun checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        selfDamage: Float,
        targetInfo: TargetInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val targetDamage = calcDamage(targetInfo.entity, targetInfo.pos, targetInfo.box, crystalX, crystalY, crystalZ, mutableBlockPos)
        if (lethalOverride.get() && targetDamage - targetInfo.entity.totalHealth > lethalBalance.get() && targetDamage <= lethalMaxDamage.get()) {
            return true
        }
        if (selfDamage > breakMaxSelfDamage.get()) return false
        val minDamage: Float
        val balance: Float
        if (shouldForcePlace(targetInfo.entity)) {
            minDamage = forcePlaceMinDamage.get()
            balance = forcePlaceBalance.get()
        } else {
            minDamage = breakMinDamage.get()
            balance = breakBalance.get()
        }
        return targetDamage >= minDamage && targetDamage - selfDamage >= balance
    }

/**
    init {
        safeListener<RenderEntityEvent.Model.Pre> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                glDepthRange(0.0, 0.01)
                GuiSetting.primary.alpha(chamsAlpha).setGLColor()
                GlStateManager.disableTexture2D()
                GlStateManager.disableLighting()
                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
            }
        }

        safeListener<RenderEntityEvent.Model.Post> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                GlStateManager.enableTexture2D()
                GlStateManager.enableLighting()
            }
        }

        safeListener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                glDepthRange(0.0, 1.0)
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (paused()) return@safeListener

            if (!rotation) return@safeListener

            var placing = System.currentTimeMillis() - lastActiveTime <= 250L
            rotationInfo.get(mc.timer.tickLength.toInt() * 2)?.let {
                lastRotation = it
                placing = true
            }

            if (placing) {
                lastRotation?.let {
                    val rotation = getRotationTo(it.hitVec)
                    val diff = RotationUtils.calcAngleDiff(rotation.x, PlayerPacketManager.rotation.x)

                    if (abs(diff) <= yawSpeed) {
                        sendPlayerPacket {
                            rotate(rotation)
                        }
                    } else {
                        val clamped = diff.coerceIn(-yawSpeed, yawSpeed)
                        val newYaw = RotationUtils.normalizeAngle(PlayerPacketManager.rotation.x + clamped)

                        sendPlayerPacket {
                            rotate(Vec2f(newYaw, rotation.y))
                        }
                    }
                }
            } else {
                lastRotation = null
            }
        }
    }
**/
    private class TickTimer(val timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
        var time = System.currentTimeMillis()
        fun tick(delay: Int): Boolean {
            val current = System.currentTimeMillis()
            return current - time >= delay * timeUnit.multiplier
        }
        fun tick(delay: Long): Boolean {
            val current = System.currentTimeMillis()
            return current - time >= delay * timeUnit.multiplier
        }
        fun tick(delay: Int, unit: TimeUnit): Boolean {
            val current = System.currentTimeMillis()
            return current - time >= delay * unit.multiplier
        }
        fun tick(delay: Long, unit: TimeUnit): Boolean {
            val current = System.currentTimeMillis()
            return current - time >= delay * unit.multiplier
        }
        fun tickAndReset(delay: Int): Boolean {
            val current = System.currentTimeMillis()
            return if (current - time >= delay * timeUnit.multiplier) {
                time = current
                true
            } else {
                false
            }
        }
        fun tickAndReset(delay: Long): Boolean {
            val current = System.currentTimeMillis()
            return if (current - time >= delay * timeUnit.multiplier) {
                time = current
                true
            } else {
                false
            }
        }
        fun tickAndReset(delay: Int, unit: TimeUnit): Boolean {
            val current = System.currentTimeMillis()
            return if (current - time >= delay * unit.multiplier) {
                time = current
                true
            } else {
                false
            }
        }
        fun tickAndReset(delay: Long, unit: TimeUnit): Boolean {
            val current = System.currentTimeMillis()
            return if (current - time >= delay * unit.multiplier) {
                time = current
                true
            } else {
                false
            }
        }
        fun reset() {
            time = System.currentTimeMillis()
        }
        fun reset(offset: Long) {
            time = System.currentTimeMillis() + offset
        }
        fun reset(offset: Int) {
            time = System.currentTimeMillis() + offset
        }
    }
    enum class TimeUnit(val multiplier: Long) {
        MILLISECONDS(1L),
        TICKS(50L),
        SECONDS(1000L),
        MINUTES(60000L);
    }
    class CircularArray<E> private constructor(private val array: Array<Any?>, filled: Boolean) : MutableList<E> {
        constructor(size: Int) : this(arrayOfNulls(size), false)
        constructor(size: Int, defaultValue: E) : this(Array(size) { defaultValue }, true)
        constructor(size: Int, init: (Int) -> E) : this(Array(size, init), true)
        override var size = if (filled) array.size else 0; private set
        private var index = 0
        override fun isEmpty(): Boolean {
            return size == 0
        }
        @Suppress("UNCHECKED_CAST")
        override fun get(index: Int): E {
            checkIndex(index)
            return array[index] as E
        }
        override fun indexOf(element: E): Int {
            for (i in 0 until size) {
                if (array[i] == element) return i
            }
            return -1
        }
        override fun lastIndexOf(element: E): Int {
            for (i in size - 1 downTo 0) {
                if (array[i] == element) return i
            }
            return -1
        }
        override fun addAll(index: Int, elements: Collection<E>): Boolean {
            throw UnsupportedOperationException()
        }
        override fun addAll(elements: Collection<E>): Boolean {
            for (element in elements) {
                add(element)
            }
            return true
        }
        override fun add(index: Int, element: E) {
            checkIndexArray(index)
            if (size < array.size) {
                when (index) {
                    0 -> {
                        if (isEmpty()) {
                            add(element)
                        } else {
                            moveBackward(index)
                            add0(index, element)
                        }
                    }
                    size -> {
                        add0(index, element)
                    }
                    else -> {
                        moveBackward(index)
                        add0(index, element)
                    }
                }
            } else {
                if (index > 0) {
                    move(0, 1, index - 1)
                }
                val lastIndex = size - 1
                val end = lastIndex - index
                if (end > 0) {
                    array[0] = array[lastIndex]
                    move(index, index + 1, end)
                }
                array[index] = element
            }
        }
        override fun add(element: E): Boolean {
            add0(index, element)
            return true
        }
        private fun add0(index: Int, element: E) {
            array[index] = element
            this.index = (this.index + 1) % array.size
            if (size < array.size) size++
        }
        @Suppress("UNCHECKED_CAST")
        override fun set(index: Int, element: E): E {
            checkIndex(index)
            val prev = array[index]
            array[index] = element
            return prev as E
        }
        override fun retainAll(elements: Collection<E>): Boolean {
            var removed = false
            removeIf { element ->
                !elements.contains(element).also { removed = removed || it }
            }
            return removed
        }
        override fun removeAll(elements: Collection<E>): Boolean {
            var removed = false
            for (element in elements) {
                removed = remove(element) || removed
            }
            return removed
        }
        override fun remove(element: E): Boolean {
            for (i in 0 until size) {
                if (array[i] == element) removeAt0(i)
                return true
            }
            return false
        }
        override fun removeAt(index: Int): E {
            checkIndex(index)
            return removeAt0(index)
        }
        private fun removeAt0(index: Int): E {
            val element = get(index)
            val numMoved = size - index - 1
            if (numMoved > 0) {
                move(index + 1, index, numMoved)
            }
            array[size - 1] = null
            size--
            return element
        }
        override fun clear() {
            this.size = 0
            this.index = 0
        }
        override fun contains(element: E): Boolean {
            return array.contains(element)
        }
        override fun containsAll(elements: Collection<E>): Boolean {
            return elements.all(array::contains)
        }
        @Suppress("UNCHECKED_CAST")
        override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
            checkIndex(fromIndex)
            checkIndex(toIndex)
            return ArrayList<E>(toIndex - fromIndex).apply {
                for (i in fromIndex until toIndex) {
                    add(array[i] as E)
                }
            }
        }
        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private var index = 0
            override fun hasNext(): Boolean = index in 0 until size
            override fun next(): E {
                if (index >= size) {
                    throw NoSuchElementException("Index: $index")
                }
                return get(index++)
            }
            override fun remove() {
                removeAt(index-- - 1)
            }
        }
        override fun listIterator() = listIterator(0)
        override fun listIterator(index: Int) = object : MutableListIterator<E> {
            private var index0 = index
            override fun hasPrevious() = index0 in 0 until size
            override fun hasNext() = index0 in 0 until size
            override fun previousIndex() = index0
            override fun nextIndex() = index0
            override fun previous(): E {
                if (index0 < 0 || index0 >= size) {
                    throw NoSuchElementException("Index: $index0")
                }
                return get(index0--)
            }
            override fun next(): E {
                if (index0 < 0 || index0 >= size) {
                    throw NoSuchElementException("Index: $index0")
                }
                return get(index0++)
            }
            override fun add(element: E) {
                add(index0, element)
            }
            override fun remove() {
                removeAt(index0-- - 1)
            }
            override fun set(element: E) {
                set(index, element)
            }
        }
        override fun toString(): String {
            return StringBuilder().run {
                val lastIndex = size - 1
                append('[')
                for (i in 0..lastIndex) {
                    append(array[i])
                    if (i != lastIndex) append(", ")
                }
                append(']')
                toString()
            }
        }
        private fun checkIndexArray(index: Int) {
            if (index >= array.size || index > size) {
                throw IndexOutOfBoundsException("$index")
            }
        }
        private fun checkIndex(index: Int) {
            if (size == 0 || index < 0 || index >= size) {
                throw IndexOutOfBoundsException("$index")
            }
        }
        private fun moveBackwardFull(index: Int) {
            move(index, index + 1, size - index - 1)
        }
        private fun moveBackward(index: Int) {
            move(index, index + 1, size - index)
        }
        private fun move(from: Int, to: Int, size: Int) {
            System.arraycopy(array, from, array, to, size)
        }
        companion object {
            fun CircularArray<Float>.average(): Float {
                if (size == 0) return 0.0f
                var sum = 0.0f
                for (i in 0 until size) {
                    sum += this[i]
                }
                return sum / size
            }
            fun CircularArray<Int>.average(): Double {
                if (size == 0) return 0.0
                var sum = 0.0
                for (i in 0 until size) {
                    sum += this[i]
                }
                return sum / size
            }
        }
    }
    private class DamageReduction(entity: EntityLivingBase) {
        private val armorValue: Float = entity.totalArmorValue.toFloat()
        private val toughness: Float
        private val resistance: Float
        private val blastReduction: Float
        init {
            toughness = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue
                .toFloat()
            val potionEffect = entity.getActivePotionEffect(MobEffects.RESISTANCE)
            resistance = if (potionEffect != null) max(1.0f - (potionEffect.amplifier + 1) * 0.2f, 0.0f) else 1.0f
            blastReduction = 1.0f - min(calcTotalEPF(entity), 20) / 25.0f
        }
        fun calcReductionDamage(damage: Float): Float {
            return CombatRules.getDamageAfterAbsorb(damage, armorValue, toughness) *
                    resistance *
                    blastReduction
        }
        companion object {
            private inline fun calcTotalEPF(entity: EntityLivingBase): Int {
                var epf = 0
                for (itemStack in entity.armorInventoryList) {
                    val nbtTagList = itemStack.enchantmentTagList
                    for (i in 0 until nbtTagList.tagCount()) {
                        val nbtTagCompound = nbtTagList.getCompoundTagAt(i)
                        val id = nbtTagCompound.getInteger("id")
                        val level = nbtTagCompound.getShort("lvl").toInt()
                        if (id == 0) {
                            // Protection
                            epf += level
                        } else if (id == 3) {
                            // Blast protection
                            epf += level * 2
                        }
                    }
                }
                return epf
            }
        }
    }
    private inline fun getTargetEntities(): Sequence<TargetInfo> {
        val rangeSq = targetRange.get() * targetRange.get()
        val ticks = if (motionPredict.get()) predictTicks.get() else 0
        val list = ArrayList<TargetInfo>()
        val eyePos = mc.player.getPositionEyes(1F)

        mc.world.loadedEntityList
            .filter { EntityUtils.isSelected(it,true) && it.distanceSqTo(eyePos) > rangeSq }
            .forEach { list.add(getTargetInfo(it as EntityLivingBase, ticks)) }

        list.sortBy { mc.player.getDistanceSqToEntity(it.entity) }

        return list.asSequence()
            .filter { it.entity.isEntityAlive }
            .take(maxTargets.get())
    }
    private inline fun getTargetInfo(entity: EntityLivingBase, ticks: Int): TargetInfo {
        val motionX = (entity.posX - entity.lastTickPosX).coerceIn(-0.6, 0.6)
        val motionY = (entity.posY - entity.lastTickPosY).coerceIn(-0.5, 0.5)
        val motionZ = (entity.posZ - entity.lastTickPosZ).coerceIn(-0.6, 0.6)

        val entityBox = entity.entityBoundingBox
        var targetBox = entityBox

        for (tick in 0..ticks) {
            targetBox = canMove(targetBox, motionX, motionY, motionZ)
                ?: canMove(targetBox, motionX, 0.0, motionZ)
                        ?: canMove(targetBox, 0.0, motionY, 0.0)
                        ?: break
        }

        val offsetX = targetBox.minX - entityBox.minX
        val offsetY = targetBox.minY - entityBox.minY
        val offsetZ = targetBox.minZ - entityBox.minZ
        val motion = Vec3d(offsetX, offsetY, offsetZ)
        val pos = entity.positionVector

        return TargetInfo(entity, pos.add(motion), targetBox, pos, motion)
    }
    private inline fun canMove(box: AxisAlignedBB, x: Double, y: Double, z: Double): AxisAlignedBB? {
        return box.offset(x, y, z).takeIf { !mc.world.collidesWithAnyBlock(it) }
    }
    private data class TargetInfo(val entity: EntityLivingBase, val pos: Vec3d, val box: AxisAlignedBB, val currentPos: Vec3d, val predictMotion: Vec3d)
    private inline fun Entity.distanceSqTo(vec3d: Vec3d)
    = ((vec3d.x - this.posX) * (vec3d.x - this.posX)) + ((vec3d.y - this.posY) * (vec3d.y - this.posY)) + ((vec3d.z - this.posZ) * (vec3d.z - this.posZ))
    private val EntityLivingBase.scaledHealth: Float
        get() = this.health + this.absorptionAmount * (this.health / this.maxHealth)
    private val EntityLivingBase.totalHealth: Float
        get() = this.health + this.absorptionAmount
    private fun getCrystalPlacingBB(pos: BlockPos): AxisAlignedBB {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }
    private fun getCrystalPlacingBB(x: Int, y: Int, z: Int): AxisAlignedBB {
        return AxisAlignedBB(
            x + 0.001, y + 1.0, z + 0.001,
            x + 0.999, y + 3.0, z + 0.999
        )
    }
    private const val FLOOR_DOUBLE_D = 1_073_741_824.0
    private const val FLOOR_DOUBLE_I = 1_073_741_824
    private const val FLOOR_FLOAT_F = 4_194_304.0f
    private const val FLOOR_FLOAT_I = 4_194_304
    private inline fun Double.fastFloor() = (this + FLOOR_DOUBLE_D).toInt() - FLOOR_DOUBLE_I
    private inline fun Float.fastFloor() = (this + FLOOR_FLOAT_F).toInt() - FLOOR_FLOAT_I
    private inline fun Double.fastCeil() = FLOOR_DOUBLE_I - (FLOOR_DOUBLE_D - this).toInt()
    private inline fun Float.fastCeil() = FLOOR_FLOAT_I - (FLOOR_FLOAT_F - this).toInt()
    @Suppress("DuplicatedCode")
    private inline fun calcPlaceInfo(checkRotation: Boolean): PlaceInfo? {
        val max = PlaceInfo.Mutable(mc.player)
        val safe = PlaceInfo.Mutable(mc.player)
        val lethal = PlaceInfo.Mutable(mc.player)
        val targets = targets.toList()
        val playerPos = mc.player.positionVector
        val playerBox = mc.player.boundingBox
        val noSuicide = noSuicide
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val targetBlocks = getPlaceablePos(checkRotation, mutableBlockPos)
        if (targets.isNotEmpty()) {
            for (pos in targetBlocks) {
                val placeBox = getCrystalPlacingBB(pos)
                val crystalX = pos.x + 0.5
                val crystalY = pos.y + 1.0
                val crystalZ = pos.z + 0.5
                val selfDamage = calcDamage(mc.player, playerPos, playerBox, crystalX, crystalY, crystalZ, mutableBlockPos)
                if (mc.player.scaledHealth - selfDamage <= noSuicide.get()) continue
                if (!lethalOverride.get() && selfDamage > placeMaxSelfDamage.get()) continue
                for ((entity, entityPos, entityBox, currentPos) in targets) {
                    if (entityBox.intersects(placeBox)) continue
                    if (placeBox.intersects(entityPos, currentPos)) continue
                    val targetDamage = calcDamage(entity, entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
                    if (lethalOverride.get() && targetDamage - entity.totalHealth > lethalBalance.get() && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxDamage.get()) {
                        lethal.update(entity, pos, selfDamage, targetDamage)
                    }
                    if (selfDamage > placeMaxSelfDamage.get()) continue
                    val minDamage: Float
                    val balance: Float
                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage.get()
                        balance = forcePlaceBalance.get()
                    } else {
                        minDamage = placeMinDamage.get()
                        balance = placeBalance.get()
                    }
                    if (targetDamage >= minDamage && targetDamage - selfDamage >= balance) {
                        if (targetDamage > max.targetDamage) {
                            max.update(entity, pos, selfDamage, targetDamage)
                        } else if (safeOverride.get() && max.targetDamage - targetDamage <= safeRange.get()
                            && max.selfDamage - selfDamage >= safeThreshold.get()) {
                            safe.update(entity, pos, selfDamage, targetDamage)
                        }
                    }
                }
            }
        }
        if (max.targetDamage - safe.targetDamage > safeRange.get()
            || max.selfDamage - safe.selfDamage <= safeThreshold.get()) {
            safe.clear(mc.player)
        }
        val placeInfo = lethal.takeValid()
            ?: safe.takeValid()
            ?: max.takeValid()
        placeInfo?.calcPlacement()
        return placeInfo
    }
    private inline fun shouldForcePlace(entity: EntityLivingBase): Boolean {
        return (!forcePlaceSword.get() || mc.player.heldItemMainhand.item !is ItemSword)
                && (entity.totalHealth <= forcePlaceHealth.get()
                || entity.realSpeed >= forcePlaceMotion.get()
                || entity.getMinArmorRate() <= forcePlaceArmorRate.get())
    }
    private inline val Entity.realSpeed get() = hypot(posX - prevPosX, posZ - prevPosZ)
    private inline fun EntityLivingBase.getMinArmorRate(): Int {
        return this.armorInventoryList.toList().asSequence()
            .filter { it.isItemStackDamageable }
            .map { ((it.maxDamage - it.itemDamage) * 100.0f / it.maxDamage.toFloat()).toInt() }
            .maxOrNull() ?: 0
    }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    private fun isResistant(blockState: IBlockState) =
        !blockState.material.isLiquid && blockState.block.getExplosionResistance(null) >= 19.7
    private inline fun calcDamage(
        entity: EntityLivingBase,
        entityPos: Vec3d,
        entityBox: AxisAlignedBB,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val isPlayer = entity is EntityPlayer
        if (isPlayer && mc.world.difficulty == EnumDifficulty.PEACEFUL) return 0.0f
        var damage: Float
        damage = if (isPlayer
            && crystalY - entityPos.y > 1.5652173822904127
            && isResistant(mc.world.getBlockState(mutableBlockPos.setPos(crystalX.fastFloor(), crystalY.fastFloor() - 1, crystalZ.fastFloor())))
        ) {
            1.0f
        } else {
            calcRawDamage(entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
        }
        if (isPlayer) damage = calcDifficultyDamage(mc.world, damage)
        return calcReductionDamage(entity, damage)
    }
    private const val DOUBLE_SIZE = 12.0f
    private const val DAMAGE_FACTOR = 42.0f
    private inline fun Vec3d.distanceTo(x: Double, y: Double, z: Double): Double {
        return distance(this.x, this.y, this.z, x, y, z)
    }
    private inline fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
    }
    private inline fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        return ((x2 - x1)*(x2 - x1)) + ((y2 - y1)*(y2 - y1)) + ((z2 - z1)*(z2 - z1))
    }
    private inline fun calcRawDamage(
        entityPos: Vec3d,
        entityBox: AxisAlignedBB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val scaledDist = entityPos.distanceTo(posX, posY, posZ).toFloat() / DOUBLE_SIZE
        if (scaledDist > 1.0f) return 0.0f

        val factor = (1.0f - scaledDist) * getExposureAmount(entityBox, posX, posY, posZ, mutableBlockPos)
        return ((factor * factor + factor) * DAMAGE_FACTOR + 1.0f)
    }
    private inline fun getExposureAmount(
        entityBox: AxisAlignedBB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val width = entityBox.maxX - entityBox.minX
        val height = entityBox.maxY - entityBox.minY
        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)
        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY
        val sizeXZ = (1.0 / gridMultiplierXZ).fastFloor()
        val sizeY = (1.0 / gridMultiplierY).fastFloor()
        val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ)) / 2.0
        var total = 0
        var count = 0
        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    val x = gridXZ * xIndex + xzOffset + entityBox.minX
                    val y = gridY * yIndex + entityBox.minY
                    val z = gridXZ * zIndex + xzOffset + entityBox.minZ
                    total++
                    if (!mc.world.fastRaytrace(x, y, z, posX, posY, posZ, 20, mutableBlockPos, function)) {
                        count++
                    }
                }
            }
        }
        return count.toFloat() / total.toFloat()
    }
    private val function: (BlockPos, IBlockState) -> FastRayTraceAction = { _, blockState ->
        if (blockState.block != Blocks.AIR && isResistant(blockState)) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }
    private inline fun calcReductionDamage(entity: EntityLivingBase, damage: Float): Float {
        val reduction = reductionMap[entity]
        return reduction?.calcReductionDamage(damage) ?: damage
    }
    private inline fun calcDifficultyDamage(world: WorldClient, damage: Float): Float {
        return when (world.difficulty) {
            EnumDifficulty.PEACEFUL -> 0.0f
            EnumDifficulty.EASY -> min(damage * 0.5f + 1.0f, damage)
            EnumDifficulty.HARD -> damage * 1.5f
            else -> damage
        }
    }
    enum class FastRayTraceAction {
        SKIP, MISS, CALC, HIT
    }
    fun World.fastRaytrace(
        startX: Double,
        startY: Double,
        startZ: Double,
        endX: Double,
        endY: Double,
        endZ: Double,
        maxAttempt: Int = 50,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: (BlockPos, IBlockState) -> FastRayTraceAction
    ): Boolean {
        var currentX = startX
        var currentY = startY
        var currentZ = startZ
        // Int start position
        var currentBlockX = currentX.fastFloor()
        var currentBlockY = currentY.fastFloor()
        var currentBlockZ = currentZ.fastFloor()
        // Raytrace start block
        mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val startBlockState = getBlockState(mutableBlockPos)
        when (function.invoke(mutableBlockPos, startBlockState)) {
            FastRayTraceAction.MISS -> return false
            FastRayTraceAction.CALC -> if (startBlockState.fastRaytrace(this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
            FastRayTraceAction.HIT -> return true
            else -> {

            }
        }
        // Int end position
        val endBlockX = endX.fastFloor()
        val endBlockY = endY.fastFloor()
        val endBlockZ = endZ.fastFloor()
        var count = maxAttempt
        while (count-- >= 0) {
            if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
                return false
            }
            var nextX = 999
            var nextY = 999
            var nextZ = 999
            var stepX = 999.0
            var stepY = 999.0
            var stepZ = 999.0
            val diffX = endX - currentX
            val diffY = endY - currentY
            val diffZ = endZ - currentZ
            if (endBlockX > currentBlockX) {
                nextX = currentBlockX + 1
                stepX = (nextX - currentX) / diffX
            } else if (endBlockX < currentBlockX) {
                nextX = currentBlockX
                stepX = (nextX - currentX) / diffX
            }
            if (endBlockY > currentBlockY) {
                nextY = currentBlockY + 1
                stepY = (nextY - currentY) / diffY
            } else if (endBlockY < currentBlockY) {
                nextY = currentBlockY
                stepY = (nextY - currentY) / diffY
            }
            if (endBlockZ > currentBlockZ) {
                nextZ = currentBlockZ + 1
                stepZ = (nextZ - currentZ) / diffZ
            } else if (endBlockZ < currentBlockZ) {
                nextZ = currentBlockZ
                stepZ = (nextZ - currentZ) / diffZ
            }
            if (stepX < stepY && stepX < stepZ) {
                currentX = nextX.toDouble()
                currentY += diffY * stepX
                currentZ += diffZ * stepX

                currentBlockX = nextX - (endBlockX - currentBlockX ushr 31)
                currentBlockY = currentY.fastFloor()
                currentBlockZ = currentZ.fastFloor()
            } else if (stepY < stepZ) {
                currentX += diffX * stepY
                currentY = nextY.toDouble()
                currentZ += diffZ * stepY

                currentBlockX = currentX.fastFloor()
                currentBlockY = nextY - (endBlockY - currentBlockY ushr 31)
                currentBlockZ = currentZ.fastFloor()
            } else {
                currentX += diffX * stepZ
                currentY += diffY * stepZ
                currentZ = nextZ.toDouble()

                currentBlockX = currentX.fastFloor()
                currentBlockY = currentY.fastFloor()
                currentBlockZ = nextZ - (endBlockZ - currentBlockZ ushr 31)
            }
            mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
            val blockState = getBlockState(mutableBlockPos)
            when (function.invoke(mutableBlockPos, blockState)) {
                FastRayTraceAction.MISS -> return false
                FastRayTraceAction.CALC -> if (blockState.fastRaytrace(this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
                FastRayTraceAction.HIT -> return true
                else -> {

                }
            }
        }
        return false
    }
    private fun IBlockState.fastRaytrace(
        world: World,
        blockPos: BlockPos.MutableBlockPos,
        x1: Double,
        y1: Double,
        z1: Double,
        x2: Double,
        y2: Double,
        z2: Double
    ): Boolean {
        val x1f = (x1 - blockPos.x).toFloat()
        val y1f = (y1 - blockPos.y).toFloat()
        val z1f = (z1 - blockPos.z).toFloat()
        val box = this.getBoundingBox(world, blockPos)
        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()
        val xDiff = (x2 - blockPos.x).toFloat() - x1f
        val yDiff = (y2 - blockPos.y).toFloat() - y1f
        val zDiff = (z2 - blockPos.z).toFloat() - z1f
        if (xDiff * xDiff >= 1.0E-7f) {
            var factor = (minX - x1f) / xDiff
            if (factor !in 0.0f..1.0f) factor = (maxX - x1f) / xDiff

            if (factor in 0.0f..1.0f && y1f + yDiff * factor in minY..maxY && z1f + zDiff * factor in minZ..maxZ) {
                return true
            }
        }
        if (yDiff * yDiff >= 1.0E-7f) {
            var factor = (minY - y1f) / yDiff
            if (factor !in 0.0f..1.0f) factor = (maxY - y1f) / yDiff

            if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX && z1f + zDiff * factor in minZ..maxZ) {
                return true
            }
        }
        if (zDiff * zDiff >= 1.0E-7) {
            var factor = (minZ - z1f) / zDiff
            if (factor !in 0.0f..1.0f) factor = (maxZ - z1f) / zDiff

            if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX && y1f + yDiff * factor in minY..maxY) {
                return true
            }
        }
        return false
    }
    private fun World.rayTraceVisible(
        start: Vec3d,
        endX: Double,
        endY: Double,
        endZ: Double,
        maxAttempt: Int = 50,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return !fastRaytrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos) { pos, blockState ->
            if (blockState.getCollisionBoundingBox(this, pos) != null) {
                FastRayTraceAction.CALC
            } else {
                FastRayTraceAction.SKIP
            }
        }
    }
    private inline fun checkPlaceRotation(pos: BlockPos, eyePos: Vec3d, sight: Vec3d): Boolean {
        if (AxisAlignedBB(pos).calculateIntercept(eyePos, sight) != null) return true
        return placeRotationRange.get() != 0.0f
                && checkRotationDiff(getRotationTo(eyePos, pos.toVec3dCenter()), placeRotationRange.get())
    }
    private fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }
    private inline fun checkRotationDiff(rotation: Vec2f, range: Float): Boolean {
        val serverSide = RotationUtils.serverRotation
        return calcAbsAngleDiff(rotation.x, serverSide.yaw) <= range
                && calcAbsAngleDiff(rotation.y, serverSide.pitch) <= range
    }
    private const val PI_FLOAT = 3.14159265358979323846f
    private inline fun Float.toRadian() = this / 180.0f * PI_FLOAT
    private inline fun Double.toRadian() = this / 180.0 * PI
    data class Vec2d(val x: Double = 0.0, val y: Double = 0.0) {
        constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
        constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y)
        constructor(vec2d: Vec2d) : this(vec2d.x, vec2d.y)
        fun toRadians() = Vec2d(x.toRadian(), y.toRadian())
        fun length() = hypot(x, y)
        fun lengthSquared() = (this.x.pow(2) + this.y.pow(2))
        operator fun div(vec2d: Vec2d) = div(vec2d.x, vec2d.y)
        operator fun div(divider: Double) = div(divider, divider)
        fun div(x: Double, y: Double) = Vec2d(this.x / x, this.y / y)
        operator fun times(vec2d: Vec2d) = times(vec2d.x, vec2d.y)
        operator fun times(multiplier: Double) = times(multiplier, multiplier)
        fun times(x: Double, y: Double) = Vec2d(this.x * x, this.y * y)
        operator fun minus(vec2d: Vec2d) = minus(vec2d.x, vec2d.y)
        operator fun minus(sub: Double) = minus(sub, sub)
        fun minus(x: Double, y: Double) = plus(-x, -y)
        operator fun plus(vec2d: Vec2d) = plus(vec2d.x, vec2d.y)
        operator fun plus(add: Double) = plus(add, add)
        fun plus(x: Double, y: Double) = Vec2d(this.x + x, this.y + y)
        fun toVec2f() = Vec2f(x.toFloat(), y.toFloat())
        override fun toString(): String {
            return "Vec2d[${this.x}, ${this.y}]"
        }
        companion object {
            @JvmField
            val ZERO = Vec2d(0.0, 0.0)
        }
    }
    @JvmInline
    value class Vec2f private constructor(val bits: Long) {
        constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFFFFFF))
        constructor(entity: Entity) : this(entity.rotationYaw, entity.rotationPitch)
        constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
        constructor(vec2d: Vec2d) : this(vec2d.x.toFloat(), vec2d.y.toFloat())
        val x: Float
            get() = getX(bits)
        val y: Float
            get() = getY(bits)
        fun toRadians(): Vec2f {
            return Vec2f(x.toRadian(), y.toRadian())
        }
        fun length() = hypot(x, y)
        fun lengthSquared() = (x.pow(2) + y.pow(2))
        operator fun div(vec2f: Vec2f) = div(vec2f.x, vec2f.y)
        operator fun div(divider: Float) = div(divider, divider)
        fun div(x: Float, y: Float) = Vec2f(this.x / x, this.y / y)
        operator fun times(vec2f: Vec2f) = times(vec2f.x, vec2f.y)
        operator fun times(multiplier: Float) = times(multiplier, multiplier)
        fun times(x: Float, y: Float) = Vec2f(this.x * x, this.y * y)
        operator fun minus(vec2f: Vec2f) = minus(vec2f.x, vec2f.y)
        operator fun minus(value: Float) = minus(value, value)
        fun minus(x: Float, y: Float) = plus(-x, -y)
        operator fun plus(vec2f: Vec2f) = plus(vec2f.x, vec2f.y)
        operator fun plus(value: Float) = plus(value, value)
        fun plus(x: Float, y: Float) = Vec2f(this.x + x, this.y + y)
        fun toVec2d() = Vec2d(x.toDouble(), y.toDouble())
        operator fun component1() = x
        operator fun component2() = y
        companion object {
            val ZERO = Vec2f(0f, 0f)
            @JvmStatic
            fun getX(bits: Long): Float {
                return Float.fromBits((bits shr 32).toInt())
            }
            @JvmStatic
            fun getY(bits: Long): Float {
                return Float.fromBits((bits and 0xFFFFFFFF).toInt())
            }
        }
    }
    private fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }
    private fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }
    private inline fun Float.toDegree() = this * 180.0f / PI_FLOAT
    private inline fun Double.toDegree() = this * 180.0 / PI
    private fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = normalizeAngle(atan2(vec.z, vec.x).toDegree() - 90.0)
        val pitch = normalizeAngle(-atan2(vec.y, xz).toDegree())
        return Vec2f(yaw, pitch)
    }
    private inline fun checkBreakRange(
        x: Double,
        y: Double,
        z: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return mc.player.eyeDistanceSq(x, y, z) <= (breakRange.get() * breakRange.get())
                && (mc.player.getDistanceSq(x, y, z) <= (wallRange.get() * wallRange.get()) || mc.world.rayTraceVisible(mc.player.getPositionEyes(1F), x, y + 1.7, z, 20, mutableBlockPos))
    }
    private inline fun Entity.eyeDistanceSq(x: Double, y: Double, z: Double): Double {
        return distanceSq(this.posX, this.posY + this.eyeHeight, this.posZ, x, y, z)
    }
    private fun canPlaceCrystalOn(pos: BlockPos): Boolean {
        val block = mc.world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }
    private fun isValidMaterial(blockState: IBlockState): Boolean {
        return !blockState.material.isLiquid && blockState.material.isReplaceable
    }
    private inline fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, add: BlockPos): BlockPos.MutableBlockPos {
        return this.setPos(set.x + add.x, set.y + add.y, set.z + add.z)
    }
    private inline fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, x: Int, y: Int, z: Int): BlockPos.MutableBlockPos {
        return this.setPos(set.x + x, set.y + y, set.z + z)
    }
    private inline fun isPlaceable(pos: BlockPos, newPlacement: Boolean, mutableBlockPos: BlockPos.MutableBlockPos): Boolean {
        if (!canPlaceCrystalOn(pos)) {
            return false
        }
        val posUp = mutableBlockPos.setAndAdd(pos, 0, 1, 0)
        return if (newPlacement) {
            mc.world.isAirBlock(posUp)
        } else {
            isValidMaterial(mc.world.getBlockState(posUp))
                    && isValidMaterial(mc.world.getBlockState(posUp.add(0, 1, 0)))
        }
    }
    private inline fun isValidPos(
        single: Boolean,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        if (!isPlaceable(pos, newPlacement.get(), mutableBlockPos)) {
            return false
        }
        val minX = pos.x + 0.001
        val minY = pos.y + 1.0
        val minZ = pos.z + 0.001
        val maxX = pos.x + 0.999
        val maxY = pos.y + 3.0
        val maxZ = pos.z + 0.999
        for (entity in mc.world.loadedEntityList) {
            if (!entity.isEntityAlive) continue
            if (!entity.entityBoundingBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)) continue
            if (!single) return false
            if (entity !is EntityEnderCrystal) return false
            if (!checkBreakRange(entity, mutableBlockPos)) return false
        }
        return true
    }
    private inline fun checkBreakRange(
        entity: EntityEnderCrystal,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return checkBreakRange(entity.posX, entity.posY, entity.posZ, mutableBlockPos)
    }
    private fun Rotation.toViewVec(): Vec3d {
        val yawRad = this.yaw.toDouble().toRadian()
        val pitchRag = this.pitch.toDouble().toRadian()
        val yaw = -yawRad - PI_FLOAT
        val pitch = -pitchRag
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)
        return Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }
    private inline fun getPlaceablePos(
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<BlockPos> {
        val range = placeRange
        val rangeSq = range.get() * range.get()
        val wallRangeSq = wallRange.get() * wallRange.get()
        val single = placeMode equal "Single"
        val floor = range.get().fastFloor()
        val ceil = range.get().fastCeil()
        val list = ArrayList<BlockPos>()
        val pos = BlockPos.MutableBlockPos()
        val feetPos = mc.player.positionVector
        val feetXInt = feetPos.x.fastFloor()
        val feetYInt = feetPos.y.fastFloor()
        val feetZInt = feetPos.z.fastFloor()
        val eyePos = mc.player.getPositionEyes(1F)
        val sight = eyePos.add(RotationUtils.serverRotation.toViewVec().scale(8.0))
        for (x in feetXInt - floor..feetXInt + ceil) {
            for (z in feetZInt - floor..feetZInt + ceil) {
                for (y in feetYInt - floor..feetYInt + ceil) {
                    pos.setPos(x, y, z)
                    if (!mc.world.worldBorder.contains(pos)) continue
                    val crystalX = pos.x + 0.5
                    val crystalY = pos.y + 1.0
                    val crystalZ = pos.z + 0.5
                    if (eyePos.squareDistanceTo(crystalX, crystalY, crystalZ) > rangeSq) continue
                    if (feetPos.squareDistanceTo(crystalX, crystalY, crystalZ) > wallRangeSq
                        && !mc.world.rayTraceVisible(eyePos, crystalX, crystalY + 1.7, crystalZ, 20, mutableBlockPos)) continue
                    if (!isValidPos(single, pos, mutableBlockPos)) continue
                    if (checkRotation && !checkPlaceRotation(pos, eyePos, sight)) continue
                    list.add(pos.toImmutable())
                }
            }
        }
        return list
    }
    private data class Vec3f(val x: Float, val y: Float, val z: Float) {
        companion object {
            @JvmField
            val ZERO = Vec3f(0.0f, 0.0f, 0.0f)
        }
    }
    private inline fun BlockPos.toVec3dCenter() = Vec3d(this.x + 0.5, this.y + 0.5, this.z + 0.5)
    private inline fun BlockPos.toVec3dCenter(xOffset: Double, yOffset: Double, zOffset: Double) =
        Vec3d(this.x + 0.5 + xOffset, this.y + 0.5 + yOffset, this.z + 0.5 + zOffset)
    private inline fun calcDirection(eyePos: Vec3d, hitVec: Vec3d): EnumFacing {
        val x = hitVec.x - eyePos.x
        val y = hitVec.y - eyePos.y
        val z = hitVec.z - eyePos.z
        return EnumFacing.HORIZONTALS.maxByOrNull {
            x * it.directionVec.x + y * it.directionVec.y + z * it.directionVec.z
        } ?: EnumFacing.NORTH
    }
    private open class PlaceInfo(
        open val target: EntityLivingBase,
        open val blockPos: BlockPos,
        open val selfDamage: Float,
        open val targetDamage: Float,
        open val side: EnumFacing,
        open val hitVecOffset: Vec3f,
        open val hitVec: Vec3d
    ) {
        class Mutable(
            target: EntityLivingBase
        ) : PlaceInfo(
            target,
            BlockPos.ORIGIN,
            Float.MAX_VALUE,
            forcePlaceMinDamage.get(),
            EnumFacing.UP,
            Vec3f.ZERO,
            Vec3d.ZERO
        ) {
            override var target = target; private set
            override var blockPos = super.blockPos; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set
            override var side = super.side; private set
            override var hitVecOffset = super.hitVecOffset; private set
            override var hitVec = super.hitVec; private set
            fun update(
                target: EntityLivingBase,
                blockPos: BlockPos,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.target = target
                this.blockPos = blockPos
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }
            fun clear(player: EntityPlayerSP) {
                update(player, BlockPos.ORIGIN, Float.MAX_VALUE, forcePlaceMinDamage.get())
            }
            fun calcPlacement() {
                when (placeBypass.get()) {
                    "Up" -> {
                        side = EnumFacing.UP
                        hitVecOffset = Vec3f(0.5f, 1.0f, 0.5f)
                        hitVec = Vec3d(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5)
                    }
                    "Down" -> {
                        side = EnumFacing.DOWN
                        hitVecOffset = Vec3f(0.5f, 0.0f, 0.5f)
                        hitVec = Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
                    }
                    "Closest" -> {
                        side = calcDirection(mc.player.getPositionEyes(1F), blockPos.toVec3dCenter())
                        val directionVec = side.directionVec
                        val x = directionVec.x * 0.5f + 0.5f
                        val y = directionVec.y * 0.5f + 0.5f
                        val z = directionVec.z * 0.5f + 0.5f
                        hitVecOffset = Vec3f(x, y, z)
                        hitVec = blockPos.toVec3dCenter(x.toDouble(), y.toDouble(), z.toDouble())
                    }
                }
            }
            fun takeValid(): Mutable? {
                return this.takeIf {
                    target != mc.player
                            && selfDamage != Float.MAX_VALUE
                            && targetDamage != forcePlaceMinDamage.get()
                }
            }
        }
    }
**/

    /** LiquidBounce
    private val swing = BooleanValue("Swing",true)

    private val place = BooleanValue("Place",true)
    private val placeRange = FloatValue("PlaceRange", 4.5F, 1.0F, 5.0F)
    private val placeMinEfficiency = FloatValue("PlaceMinEfficiency", 0.1F, 0.0F, 5.0F)

    private val destroy = BooleanValue("Destroy",true)
    private val destroyRange = FloatValue("DestroyRange", 4.5F, 1.0F, 5.0F)
    private val destroyMinEfficiency = FloatValue("DestroyMinEfficiency", 0.1F, 0.0F, 5.0F)

    private val selfPreservation = BooleanValue("SelfPreservation",true)
    private val selfDamageWeight = FloatValue("SelfDamageWeight", 2.0F, 0.0F, 10.0F)
    private val friendDamageWeight = FloatValue("FriendDamageWeight", 1.0F, 0.0F, 10.0F)

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (destroy.get()) destroyerTick()
        if (place.get()) placerTick()
    }
    private fun raytraceUpperBlockSide(
        eyes: Vec3d,
        range: Double,
        wallsRange: Double,
        expectedTarget: BlockPos
    ): VecRotation? {
        val preferredSpot = RotationUtils.getCenter(AxisAlignedBB(expectedTarget))
        val preferredRotation = RotationUtils.toRotation(preferredSpot, false)

        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        var visibleRot: VecRotation? = null
        var notVisibleRot: VecRotation? = null

        val vec3d = Vec3d(expectedTarget).add(Vec3d(0.0, 0.9, 0.0))

        for (x in 0.1..0.9 step 0.1) {
            for (z in 0.1..0.9 step 0.1) {
                val vec3 = vec3d.add(Vec3d(x, 0.0, z))

                // skip because of out of range
                val distance = eyes.squareDistanceTo(vec3)

                if (distance > rangeSquared) {
                    continue
                }

                // check if target is visible to eyes
                val visible = facingBlock(eyes, vec3, expectedTarget, EnumFacing.UP)

                // skip because not visible in range
                if (!visible && distance > wallsRangeSquared) {
                    continue
                }
                val rotation = RotationUtils.toRotation(vec3,false)
                if (visible) {
                    // Calculate next spot to preferred spot
                    if (visibleRot == null || RotationUtils.getRotationDifference(rotation, preferredRotation) < RotationUtils.getRotationDifference(
                            visibleRot.rotation,
                            preferredRotation
                        )
                    ) {
                        visibleRot = VecRotation(vec3, rotation)
                    }
                } else {
                    // Calculate next spot to preferred spot
                    if (notVisibleRot == null || RotationUtils.getRotationDifference(
                            rotation,
                            preferredRotation
                        ) < RotationUtils.getRotationDifference(notVisibleRot.rotation, preferredRotation)
                    ) {
                        notVisibleRot = VecRotation(vec3, rotation)
                    }
                }
            }
        }

        return visibleRot ?: notVisibleRot
    }
    private fun raytraceBlock(range: Double,rotation: Rotation): RayTraceResult? {
        val start = Vec3d(mc.player!!.posX, mc.player!!.posY + 1.62, mc.player!!.posZ)
        val entityLookVec = mc.player!!.getVectorForRotation(rotation.pitch,rotation.yaw) ?: return null
        val end = start.addVector(entityLookVec.x * range, entityLookVec.y * range, entityLookVec.z * range)
        return mc.world!!.rayTraceBlocks(start, end)
    }
    private fun canSeeBox(
        eyes: Vec3d,
        box: AxisAlignedBB,
        range: Double,
        wallsRange: Double,
        expectedTarget: BlockPos? = null
    ): Boolean {
        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        for (x in 0.1..0.9 step 0.1) {
            for (y in 0.1..0.9 step 0.1) {
                for (z in 0.1..0.9 step 0.1) {
                    val vec3 = Vec3d(
                        box.minX + (box.maxX - box.minX) * x,
                        box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z
                    )

                    // skip because of out of range
                    val distance = eyes.squareDistanceTo(vec3)

                    if (distance > rangeSquared) {
                        continue
                    }

                    // check if target is visible to eyes
                    val visible = if (expectedTarget != null) {
                        facingBlock(eyes, vec3, expectedTarget)
                    } else {
                        isVisible(vec3)
                    }

                    // skip because not visible in range
                    if (!visible && distance > wallsRangeSquared) {
                        continue
                    }

                    return true
                }
            }
        }

        return false
    }
    private fun clickBlockWithSlot(
        player: EntityPlayerSP,
        rayTraceResult: RayTraceResult,
        slot: Int
    ) {
        val prevHotbarSlot = player.inventory.currentItem

        player.inventory.currentItem = slot

        if (slot != prevHotbarSlot) {
            player.connection.sendPacket(CPacketHeldItemChange(slot))
        }

        if (mc.playerController.processRightClickBlock(
                mc.player!!,
                mc.world!!,
                rayTraceResult.blockPos,
                rayTraceResult.sideHit,
                rayTraceResult.hitVec,
                EnumHand.MAIN_HAND
            )  == EnumActionResult.SUCCESS
        ) {
            player.swingArm(EnumHand.MAIN_HAND)
        }

        if (slot != prevHotbarSlot) {
            player.connection.sendPacket(CPacketHeldItemChange(prevHotbarSlot))
        }

        player.inventory.currentItem = prevHotbarSlot
    }

    private fun findHotbarSlot(item: Item): Int? = findHotbarSlot { it.item == item }

    private fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
        val player = mc.player ?: return null

        return (0..8).firstOrNull { predicate(player.inventory.getStackInSlot(it)) }
    }
    private fun Entity.attack(swing: Boolean) {
        val player = mc.player ?: return
        val network = mc.connection ?: return

        EventManager.callEvent(AttackEvent(this))

        // Swing before attacking (on 1.8)
        if (swing && ViaVersion.nowVersion == ProtocolCollection.R1_8.protocolVersion.version) {
            player.swingArm(EnumHand.MAIN_HAND)
        }

        network.sendPacket(CPacketUseEntity(this))

        // Swing after attacking (on 1.9+)
        if (swing && ViaVersion.nowVersion != ProtocolCollection.R1_8.protocolVersion.version) {
            player.swingArm(EnumHand.MAIN_HAND)
        }
    }
    private infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
        require(start.isFinite())
        require(endInclusive.isFinite())
        require(step > 0.0) { "Step must be positive, was: $step." }
        val sequence = generateSequence(start) { previous ->
            if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
            val next = previous + step
            if (next > endInclusive) null else next
        }
        return sequence.asIterable()
    }
    private inline fun searchBlocksInRadius(radius: Float, filter: (BlockPos, IBlockState) -> Boolean): List<Pair<BlockPos, IBlockState>> {
        val blocks = mutableListOf<Pair<BlockPos, IBlockState>>()

        val thePlayer = mc.player ?: return blocks

        val playerPos = thePlayer.positionVector
        val radiusSquared = radius * radius
        val radiusInt = radius.toInt()

        for (x in radiusInt downTo -radiusInt) {
            for (y in radiusInt downTo -radiusInt) {
                for (z in radiusInt downTo -radiusInt) {
                    val blockPos = BlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y, thePlayer.posZ.toInt() + z)
                    val state = mc.world.getBlockState(blockPos) ?: continue

                    if (!filter(blockPos, state)) {
                        continue
                    }
                    if (Vec3d(blockPos).squareDistanceTo(playerPos) > radiusSquared)
                        continue

                    blocks.add(Pair(blockPos, state))
                }
            }
        }

        return blocks
    }
    private fun facingBlock(eyes: Vec3d, vec3: Vec3d, blockPos: BlockPos, expectedSide: EnumFacing? = null): Boolean {
        val searchedPos = mc.world?.rayTraceBlocks(
            eyes,
            vec3
        ) ?: return false

        if (searchedPos.typeOfHit != RayTraceResult.Type.BLOCK || (expectedSide != null && searchedPos.sideHit != expectedSide)) {
            return false
        }

        return searchedPos.blockPos == blockPos
    }
    private fun canSeeUpperBlockSide(
        eyes: Vec3d,
        pos: BlockPos,
        range: Double,
        wallsRange: Double
    ): Boolean {
        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        val minX = pos.x.toDouble()
        val y = pos.y + 0.99
        val minZ = pos.z.toDouble()

        for (x in 0.1..0.9 step 0.4) {
            for (z in 0.1..0.9 step 0.4) {
                val vec3 = Vec3d(
                    minX + x,
                    y,
                    minZ + z
                )
                // skip because of out of range
                val distance = eyes.squareDistanceTo(vec3)
                if (distance > rangeSquared) {
                    continue
                }
                // check if target is visible to eyes
                val visible = facingBlock(eyes, vec3, pos, EnumFacing.UP)
                // skip because not visible in range
                if (!visible && distance > wallsRangeSquared) {
                    continue
                }
                return true
            }
        }
        return false
    }
    private fun AxisAlignedBB.forEachCollidingBlock(function: (x: Int, y: Int, z: Int) -> Unit) {
        val from = BlockPos(this.minX.toInt(), this.minY.toInt(), this.minZ.toInt())
        val to = BlockPos(ceil(this.maxX).toInt(), ceil(this.maxY).toInt(), ceil(this.maxZ).toInt())

        for (x in from.x until to.x) {
            for (y in from.y until to.y) {
                for (z in from.z until to.z) {
                    function(x, y, z)
                }
            }
        }
    }
    private fun getNearestPoint(eyes: Vec3d, box: AxisAlignedBB): Vec3d {
        val origin = doubleArrayOf(eyes.x, eyes.y, eyes.z)
        val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
        val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

        // It loops through every coordinate of the double arrays and picks the nearest point
        for (i in 0..2) {
            if (origin[i] > destMaxs[i]) {
                origin[i] = destMaxs[i]
            } else if (origin[i] < destMins[i]) {
                origin[i] = destMins[i]
            }
        }

        return Vec3d(origin[0], origin[1], origin[2])
    }
    private fun Entity.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
        val pos = getNearestPoint(otherPos, boundingBox)

        val xDist = pos.x - otherPos.x
        val yDist = pos.y - otherPos.y
        val zDist = pos.z - otherPos.z

        return xDist * xDist + yDist * yDist + zDist * zDist
    }
    private fun World.getEntitiesInCuboid(
        midPos: Vec3d,
        range: Double,
        predicate: (Entity) -> Boolean = { true }
    ): MutableList<Entity> {
        return getEntitiesInAABBexcluding(null, AxisAlignedBB(midPos.subtract(range, range, range), midPos.add(Vec3d(range, range, range))), predicate as (Entity?) -> Boolean)
    }
    private inline fun World.getEntitiesBoxInRange(
        midPos: Vec3d,
        range: Double,
        crossinline predicate: (Entity) -> Boolean = { true }
    ): MutableList<Entity> {
        val rangeSquared = range * range

        return getEntitiesInCuboid(midPos, range) { predicate(it) && it.squaredBoxedDistanceTo(midPos) <= rangeSquared }
    }
    private fun approximateExplosionDamage(
        world: World,
        pos: Vec3d
    ): Double {
        val possibleVictims = world.getEntitiesBoxInRange(pos, 6.0) { shouldTakeIntoAccount(it) && it.boundingBox.maxY > pos.y }.filterIsInstance<EntityLivingBase>()

        var totalGood = 0.0
        var totalHarm = 0.0

        for (possibleVictim in possibleVictims) {
            val dmg = getDamageFromExplosion(pos, possibleVictim) * entityDamageWeight(possibleVictim)

            if (dmg > 0)
                totalGood += dmg
            else
                totalHarm += dmg
        }

        return totalGood + totalHarm
    }

    private fun shouldTakeIntoAccount(entity: Entity): Boolean {
        return EntityUtils.isSelected(entity,true)  || entity == mc.player || (entity is EntityPlayer && (entity).isClientFriend())
    }

    private fun getDamageFromExplosion(pos: Vec3d, possibleVictim: EntityLivingBase, power: Float = 6.0F): Float {
        val explosionRange = power * 2.0F

        val pre1 = mc.world.getBlockDensity(pos, possibleVictim.entityBoundingBox) * (1.0F - sqrt(possibleVictim.positionVector.squareDistanceTo(pos).toFloat()) / explosionRange)

        val preprocessedDamage = floor((pre1 * pre1 + pre1) / 2.0F * 7.0F * explosionRange + 1.0F)

        return possibleVictim.applyPotionDamageCalculations(DamageSource.causeExplosionDamage(Explosion(possibleVictim.world, null, pos.x, pos.y, pos.z, power,false,true)), preprocessedDamage)
    }

    private fun entityDamageWeight(entity: Entity): Double {
        if (!selfPreservation.get()) {
            return 1.0
        }

        return when {
            entity == mc.player -> -selfDamageWeight.get().toDouble()
            entity is EntityPlayer && entity.isClientFriend() -> -friendDamageWeight.get().toDouble()
            else -> 1.0
        }
    }
    //Destroyer
    private var destroyerCurrentTarget: EntityEnderCrystal? = null
    private fun destroyerTick() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val range = destroyRange.get().toDouble()

        destroyerUpdateTarget(player, world, range)

        val target = destroyerCurrentTarget ?: return

        // find best spot (and skip if no spot was found)
        val (_,rotation) = RotationUtils.searchCenter(
            target.boundingBox,
            false,
            false,
            false,
            false,
            destroyRange.get()
        ) ?: return

        // aim on target
        RotationUtils.setTargetRotation(rotation, 0)

        if (!RotationUtils.isFaced(target, range))
            return

        target.attack(swing.get())
    }

    private fun destroyerUpdateTarget(player: EntityPlayerSP, world: World, range: Double) {
        this.destroyerCurrentTarget = world.getEntitiesBoxInRange(player.getPositionEyes(1.0F), range) { it is EntityEnderCrystal }
            .mapNotNull {
                if (!canSeeBox(
                        player.getPositionEyes(1F),
                        it.boundingBox,
                        range = range,
                        wallsRange = 0.0
                    )
                )
                    return@mapNotNull null

                val damage = approximateExplosionDamage(world, it.positionVector)

                if (damage < destroyMinEfficiency.get())
                    return@mapNotNull null

                return@mapNotNull Pair(it as EntityEnderCrystal, damage)
            }
            .maxByOrNull { it.second }
            ?.first
    }
    //Placer
    private var placerCurrentTarget: BlockPos? = null

    private fun placerTick() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val crystalSlot = findHotbarSlot(Items.END_CRYSTAL) ?: return

        placerUpdateTarget(player, world)

        val target = placerCurrentTarget ?: return

        val rotation = raytraceUpperBlockSide(player.getPositionEyes(1.0F), placeRange.get().toDouble(), 0.0, target) ?: return

        RotationUtils.setTargetRotation(rotation.rotation,0)

        val serverRotation = RotationUtils.serverRotation ?: return

        val rayTraceResult = raytraceBlock(
            placeRange.get().toDouble(),
            serverRotation
        )

        if (rayTraceResult?.typeOfHit != RayTraceResult.Type.BLOCK || rayTraceResult.blockPos != target) {
            return
        }

        clickBlockWithSlot(player, rayTraceResult, crystalSlot)
    }

    private fun placerUpdateTarget(player: EntityPlayerSP, world: World) {
        // Reset current target
        placerCurrentTarget = null

        val playerEyePos = player.getPositionEyes(1F)
        val playerPos = player.positionVector
        val range = placeRange.get().toDouble()

        val entitiesInRange = world.getEntitiesInCuboid(playerPos, range + 6.0)

        // No targets to consider? Why bother?
        if (entitiesInRange.isEmpty())
            return

        if (entitiesInRange.any { it is EntityEnderCrystal })
            return

        // The bounding box where entities are in that might body block a crystal placement
        val bodyBlockingBoundingBox = AxisAlignedBB(
            playerPos.subtract(range + 0.1, range + 0.1, range + 0.1),
            playerPos.add(Vec3d(range + 0.1, range + 0.1, range + 0.1))
        )

        val blockedPositions = HashSet<BlockPos>()

        // Disallow all positions where entities body-block them
        for (entity in entitiesInRange) {
            if (!entity.boundingBox.intersects(bodyBlockingBoundingBox))
                continue

            entity.boundingBox.forEachCollidingBlock { x, y, z ->
                blockedPositions.add(BlockPos(x, y - 1, z))
            }
        }

        // Search for blocks that are either obsidian or bedrock, not disallowed and which do not have other blocks on top
        val possibleTargets = searchBlocksInRadius(placeRange.get()) { pos, state ->
            return@searchBlocksInRadius (state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK)
                    && pos !in blockedPositions
                    && mc.world.getBlockState(pos.up()).block == Blocks.AIR
                    && canSeeUpperBlockSide(playerEyePos, pos, range, 0.0)
        }

        val bestTarget = possibleTargets
            .map { Pair(it, approximateExplosionDamage(world, Vec3d(it.first).add(Vec3d(0.5, 1.0, 0.5)))) }
            .maxByOrNull { it.second }

        // Is the target good enough?
        if (bestTarget == null || bestTarget.second < placeMinEfficiency.get())
            return

        placerCurrentTarget = bestTarget.first.first
    }
    **/
}