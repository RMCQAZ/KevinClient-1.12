package kevin.utils

import kevin.event.*
import kevin.utils.timers.MSTimer
import net.minecraft.entity.EntityLivingBase

class CombatManager : Listenable,Mc() {
    var target: EntityLivingBase? = null
        private set
    var inCombat=false
        private set
    private val lastAttackTimer = MSTimer()
    val attackedEntityList=mutableListOf<EntityLivingBase>()
    init {
        EventManager.registerListener(this)
    }
    override fun handleEvents() = true
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if(mc.player==null) return

        inCombat=false

        if(!lastAttackTimer.hasTimePassed(1000)){
            inCombat=true
            return
        }

        for (entity in mc.world.loadedEntityList) {
            if (entity is EntityLivingBase
                && entity.getDistanceToEntity(mc.player) < 7
                && EntityUtils.isSelected(entity, true)
                && !entity.isDead) {
                inCombat = true
                break
            }
        }

        if(target!=null){
            if(mc.player.getDistanceToEntity(target)>7||!inCombat||target!!.isDead){
                target=null
            }
        }

        attackedEntityList.map { it }.forEach {
            if (it.isDead) {
                EventManager.callEvent(EntityKilledEvent(it))
                attackedEntityList.remove(it)
            }
        }
    }
    @EventTarget
    fun onAttack(event: AttackEvent){
        val target=event.targetEntity

        if(target is EntityLivingBase && EntityUtils.isSelected(target,true)){
            this.target=target
            if(!attackedEntityList.contains(target))
                attackedEntityList.add(target)
        }
        lastAttackTimer.reset()
    }
    @EventTarget
    fun onWorld(event: WorldEvent){
        inCombat=false
        target=null
        attackedEntityList.clear()
    }

    fun getNearByEntity(radius: Float): EntityLivingBase?{
        return try {
            mc.world.loadedEntityList
                .filter { mc.player.getDistanceToEntity(it)<radius&&EntityUtils.isSelected(it,true) }
                .sortedBy { it.getDistanceToEntity(mc.player) }[0] as EntityLivingBase?
        }catch (e: Exception){
            null
        }
    }
}