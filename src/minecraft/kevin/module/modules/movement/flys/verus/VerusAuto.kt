package kevin.module.modules.movement.flys.verus

import kevin.KevinClient
import kevin.event.*
import kevin.hud.element.elements.Notification
import kevin.module.BooleanValue
import kevin.module.IntegerValue
import kevin.module.ListValue
import kevin.module.modules.exploit.Disabler
import kevin.module.modules.movement.flys.FlyMode
import kevin.utils.MovementUtils
import kevin.utils.timers.TickTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import kotlin.math.round

object VerusAuto : FlyMode("VerusAuto") {

    private val verusMoveMode = ListValue("VerusMoveMode", arrayOf("Walk","Jump"),"Walk")
    private val verusMoveJump: Boolean
        get() = verusMoveMode equal "Jump"
    private val verusBoost = ListValue("VerusBoost", arrayOf("None","Clip","Packet"),"Packet")
    private val verusBoostTicks = IntegerValue("VerusBoostTicks",10,1,15)
    private val verusBoostOnlyFirst = BooleanValue("VerusBoostOnlyFirst",true)
    private val verusJump = BooleanValue("VerusJump",true)
    private val verusDown = BooleanValue("VerusDown",true)
    private val verusDownNoSneak = BooleanValue("VerusDownNoSneak",true)

    private var verusState = 0
    private val verusTimer = TickTimer()
    private var playerY = .0
    private var y = 0

    override fun onEnable() {
        y = round(mc.player.posY).toInt()
        if (!(verusBoost equal "None")&&mc.player.onGround&&!verusVanilla&&mc.world.getCollisionBoxes(mc.player,mc.player.entityBoundingBox.offset(.0, 3.5, .0).expand(.0, .0, .0)).isEmpty()) {
            playerY = mc.player.posY
            when(verusBoost.get()){
                "Clip" -> {
                    mc.player.setPositionAndUpdate(
                        mc.player.posX,
                        mc.player.posY + 3.5,
                        mc.player.posZ
                    )
                    mc.player.motionX = .0
                    mc.player.motionZ = .0
                }
                "Packet" -> {
                    mc.connection!!.sendPacket(
                        CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + 3.5,
                            mc.player.posZ,
                            false
                        )
                    )
                    mc.connection!!.sendPacket(
                        CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY,
                            mc.player.posZ,
                            false
                        )
                    )
                    mc.connection!!.sendPacket(
                        CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY,
                            mc.player.posZ,
                            true
                        )
                    )
                    //mc.player.setPosition(mc.player.posX, mc.player.posY + 0.42, mc.player.posZ)
                }
            }
            verusState = 1
            mc.player.speedInAir = 0F
            mc.player.hurtTime = 0
            mc.player.onGround = false
        }
    }

    override fun onDisable() {
        verusState = 0
        mc.player.speedInAir = .02F
    }

    override fun onMotion(event: MotionEvent) {
        if (
            verusMoveJump&&
            !verusVanilla&&
            verusState!=1&&
            event.eventState== EventState.PRE&&
            !mc.gameSettings.keyBindJump.isKeyDown&&
            mc.player.jumpTicks==0&&
            !mc.player.isInWater&&
            !mc.player.isInLava&&
            !mc.player.isInWeb&&
            !mc.player.isOnLadder&&
            mc.player.posY == round(mc.player.posY)
        ) mc.player.jump()
    }

    override fun onBB(event: BlockBBEvent) {
        if (!verusVanilla&&
            verusState!=1&&
            event.block== Blocks.AIR&&
            if (verusMoveJump) (event.y < y&&event.y < mc.player!!.posY) else event.y < mc.player!!.posY&&
                    mc.player.getDistance(event.x.toDouble(),event.y.toDouble(),event.z.toDouble()) < 1.5) event.boundingBox =
            AxisAlignedBB(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x + 1.0,
                (if (verusMoveJump) (if (y.toDouble() > mc.player.posY) mc.player.posY.toInt().toDouble() else y.toDouble()) else mc.player.posY.toInt().toDouble()) - if (verusDown.get()&& GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) 1.0 else .0,
                event.z + 1.0
            )
    }

    override fun onStep(event: StepEvent) {
        if (!verusVanilla&&verusState!=1) event.stepHeight = 0f
    }

    override fun onJump(event: JumpEvent) {
        if (!verusVanilla&&verusState!=1&&!(verusJump.get()&& MovementUtils.isMoving)) event.cancelEvent()
    }

    override fun onUpdate(event: UpdateEvent) {
        if (verusState == 2) verusTimer.update()
        if (verusVanilla){
            if (fly.keepAlive.get()) mc.connection!!.sendPacket(CPacketKeepAlive())
            mc.player.motionY = 0.0
            mc.player.motionX = 0.0
            mc.player.motionZ = 0.0
            mc.player.capabilities.isFlying = false
            if (mc.gameSettings.keyBindJump.isKeyDown) mc.player.motionY += fly.speed.get()
            if (mc.gameSettings.keyBindSneak.isKeyDown) mc.player.motionY -= fly.speed.get()
            MovementUtils.strafe(fly.speed.get())
            y = round(mc.player.posY).toInt()
        }

        if (mc.player.motionY > 0.43) y = round(mc.player.posY).toInt()

        if (verusDown.get()&&GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            y = round(mc.player.posY).toInt()

        if (verusJump.get()&&mc.gameSettings.keyBindJump.isKeyDown)
            y = round(mc.player.posY).toInt()

        if (mc.player.onGround) y = round(mc.player.posY).toInt()

        if(mc.gameSettings.keyBindSneak.pressed&&verusDown.get()&&verusDownNoSneak.get()&&!verusVanilla)
            mc.gameSettings.keyBindSneak.pressed = false

        if (verusState == 1&&mc.player.posY < playerY) {
            KevinClient.hud.addNotification(Notification("Try fake ground damage boost!"),"Fly")
            verusState = 3
            mc.player.speedInAir = .02F
        }
        if (verusState == 1&&mc.player.posY > playerY&&mc.player.onGround) {
            KevinClient.hud.addNotification(Notification("Boost failed!"),"Fly")
            mc.player.speedInAir = .02F
            verusState = 2
            repeat(20){verusTimer.update()}
        }
        if (mc.player.hurtTime > 0&&((verusState == 1||verusState == 3)||(!verusBoostOnlyFirst.get()&&verusState!=0))) {
            verusState = 2
            mc.player.speedInAir = .02F
            verusTimer.reset()
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is CPacketPlayer){
            if (!verusVanilla&&mc.player.posY == round(mc.player.posY)) packet.onGround = true
        }
    }

    private val verusVanilla: Boolean
        get() = (Disabler.modeValue.get().contains("verusmove",true)&& Disabler.state) || (verusState == 2&&!verusTimer.hasTimePassed(verusBoostTicks.get()+1))
}