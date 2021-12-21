package kevin.module

import kevin.KevinClient
import kevin.event.EventManager
import kevin.event.EventTarget
import kevin.event.KeyEvent
import kevin.event.Listenable
import kevin.module.modules.Targets
import kevin.module.modules.combat.*
import kevin.module.modules.exploit.*
import kevin.module.modules.misc.*
import kevin.module.modules.movement.*
import kevin.module.modules.player.*
import kevin.module.modules.render.*
import kevin.module.modules.world.*
import kevin.module.modules.world.Timer
import net.minecraft.client.Minecraft
import java.util.*

class ModuleManager : Listenable {

    val modules = TreeSet<Module> { module1, module2 -> module1.name.compareTo(module2.name) }
    private val moduleClassMap = hashMapOf<Class<*>, Module>()

    init {
        EventManager.registerListener(this)
    }

    fun registerModules() {
        val time = System.currentTimeMillis()
        Minecraft.LOGGER.info("${KevinClient.logPrefix} [ModuleManager] Loading modules...")
        registerModules(
            Targets::class.java,
            //Combat
            AntiKnockback::class.java,
            AutoArmor::class.java,
            AutoClicker::class.java,
            AutoWeapon::class.java,
            BowAura::class.java,
            Burrow::class.java,
            Criticals::class.java,
            HitBox::class.java,
            KillAura::class.java,
            ObsAura::class.java,
            OCBreak::class.java,
            SuperKnockback::class.java,
            TeleportAttack::class.java,
            //Exploit
            AbortBreaking::class.java,
            AntiHunger::class.java,
            BoatJump::class.java,
            ClientCrasher::class.java,
            Clip::class.java,
            ForceUnicodeChat::class.java,
            Ghost::class.java,
            GhostHand::class.java,
            KeepContainer::class.java,
            Kick::class.java,
            MultiActions::class.java,
            NoPitchLimit::class.java,
            Phase::class.java,
            PingSpoof::class.java,
            Plugins::class.java,
            PortalMenu::class.java,
            ServerCrasher::class.java,
            TP::class.java,
            VehicleOneHit::class.java,
            //Misc
            AntiBot::class.java,
            AutoCommand::class.java,
            AutoL::class.java,
            ChatSuffix::class.java,
            ChestFinder::class.java,
            ComponentOnHover::class.java,
            Diastimeter::class.java,
            FakePlayer::class.java,
            InfiniteChatLength::class.java,
            KillerDetector::class.java,
            MidClickPearl::class.java,
            NameProtect::class.java,
            NoChatClear::class.java,
            NoRotateSet::class.java,
            ResourcePackSpoof::class.java,
            SuperSpammer::class.java,
            Teams::class.java,
            TotemCount::class.java,
            //Movement
            AirJump::class.java,
            AirLadder::class.java,
            AntiVoid::class.java,
            AutoWalk::class.java,
            ElytraFly::class.java,
            Fly::class.java,
            Freeze::class.java,
            HighJump::class.java,
            InvMove::class.java,
            KeepSprint::class.java,
            LiquidWalk::class.java,
            LongJump::class.java,
            NoClip::class.java,
            NoPush::class.java,
            NoSlow::class.java,
            NoWeb::class.java,
            Parkour::class.java,
            SafeWalk::class.java,
            Speed::class.java,
            Sprint::class.java,
            Step::class.java,
            Strafe::class.java,
            VehicleFly::class.java,
            WallClimb::class.java,
            WaterSpeed::class.java,
            //Player
            AntiAFK::class.java,
            AntiCactus::class.java,
            AutoFish::class.java,
            AutoRespawn::class.java,
            AutoSneak::class.java,
            AutoTool::class.java,
            AutoTotem::class.java,
            Blink::class.java,
            FastUse::class.java,
            InventoryCleaner::class.java,
            NoFall::class.java,
            Reach::class.java,
            Regen::class.java,
            //Render
            Animations::class.java,
            AntiBlind::class.java,
            BlockESP::class.java,
            BlockOverlay::class.java,
            CameraClip::class.java,
            CapeManager::class.java,
            Chams::class.java,
            ClickGui::class.java,
            DamageParticle::class.java,
            ESP::class.java,
            ExtraESP::class.java,
            FreeCam::class.java,
            FullBright::class.java,
            HUD::class.java,
            HudDesigner::class.java,
            ItemESP::class.java,
            NameTags::class.java,
            NoBob::class.java,
            NoFOV::class.java,
            NoHurtCam::class.java,
            NoSwing::class.java,
            Projectiles::class.java,
            Rotations::class.java,
            ShulkerBoxPreview::class.java,
            StorageESP::class.java,
            Tracers::class.java,
            Trajectories::class.java,
            TrueSight::class.java,
            XRay::class.java,
            //World
            Breaker::class.java,
            BugBreak::class.java,
            ChestStealer::class.java,
            FastBreak::class.java,
            FastPlace::class.java,
            NoSlowBreak::class.java,
            Nuker::class.java,
            Scaffold::class.java,
            Surround::class.java,
            TeleportUse::class.java,
            Timer::class.java
        )
        registerModule(CrystalAura)
        registerModule(Disabler)
        registerModule(AdminDetector)
        registerModule(AutoDisable)
        registerModule(HideAndSeekHack)
        registerModule(TargetStrafe)
        registerModule(Crosshair)
        registerModule(Renderer)
        registerModule(LightningDetector)
        registerModule(World)
        registerModule(HoleESP)
        Minecraft.LOGGER.info("${KevinClient.logPrefix} [ModuleManager] Loaded ${modules.size} modules,${System.currentTimeMillis()-time} MS.")
    }

    fun registerModule(module: Module) {
        modules += module
        moduleClassMap[module.javaClass] = module
        generateCommand(module)
        EventManager.registerListener(module)
    }

    private fun registerModule(moduleClass: Class<out Module>) {
        try {
            registerModule(moduleClass.newInstance())
        } catch (e: Throwable) {
            Minecraft.LOGGER.error("${KevinClient.logPrefix} [ModuleManager] Failed to load module: ${moduleClass.name} (${e.javaClass.name}: ${e.message})")
        }
    }

    @SafeVarargs
    fun registerModules(vararg modules: Class<out Module>) {
        modules.forEach(this::registerModule)
    }

    fun unregisterModule(module: Module) {
        modules.remove(module)
        moduleClassMap.remove(module::class.java)
        EventManager.unregisterListener(module)
    }

    internal fun generateCommand(module: Module) {
        if (module is Targets)
            return

        val values = module.values
        if (values.isEmpty())
            return
        KevinClient.commandManager.registerCommand(ModuleCommand(module, values))
    }

    fun getModule(moduleClass: Class<*>) = moduleClassMap[moduleClass]!!

    operator fun get(clazz: Class<*>) = getModule(clazz)

    fun getModule(moduleName: String?) = modules.find { it.name.equals(moduleName, ignoreCase = true) }

    @EventTarget
    private fun onKey(event: KeyEvent) = modules.filter { it.keyBind == event.key }.forEach { it.toggle() }

    override fun handleEvents() = true
}