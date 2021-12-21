package kevin.module.modules.misc

import kevin.module.Module
import net.minecraft.client.entity.EntityOtherPlayerMP

class FakePlayer : Module("FakePlayer", "Create a fake player.") {
    private var fakePlayer: EntityOtherPlayerMP? = null
    override fun onEnable() {
        val thePlayer = mc.player ?: return
        val faker = EntityOtherPlayerMP(mc.world!!, thePlayer.gameProfile)
        faker.rotationYawHead = thePlayer.rotationYawHead;
        faker.renderYawOffset = thePlayer.renderYawOffset;
        faker.copyLocationAndAnglesFrom(thePlayer)
        faker.rotationYawHead = thePlayer.rotationYawHead
        mc.world!!.addEntityToWorld(-1338, faker)
        fakePlayer = faker
    }
    override fun onDisable() {
        val faker = fakePlayer
        if (faker != null) {
            mc.world?.removeEntityFromWorld(faker.entityId)
            fakePlayer = null
        }
    }
}