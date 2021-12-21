package kevin.module

import kevin.KevinClient
import kevin.event.Listenable
import kevin.file.FileManager
import kevin.hud.element.elements.Notification
import kevin.utils.ClassUtils
import kevin.utils.ColorUtils.stripColor
import kevin.utils.Mc
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.SoundEvents
import org.lwjgl.input.Keyboard

open class Module(var name: String,
             var description: String = "",
             keyBind: Int = Keyboard.KEY_NONE,
             var category: ModuleCategory = ModuleCategory.MISC
) : Mc(), Listenable {
    var keyBind = keyBind
        set(keyBind) {
            field = keyBind
            if (!KevinClient.isStarting) FileManager.saveConfig(FileManager.modulesConfig)
        }
    var state = false
        set(value) {
            if (field == value) return
            onToggle(value)
            if (!KevinClient.isStarting){
                Minecraft.getMinecraft().soundHandler.playSound(
                    PositionedSoundRecord.getMasterRecord(
                        SoundEvents.UI_BUTTON_CLICK,
                        if(value) 1f else 0.6114514191981f
                    )
                )
                KevinClient.hud.addNotification(Notification("${if (value) "Enabled" else "Disabled"} $name"))
            }
            field = value
            if (value) onEnable() else onDisable()
            FileManager.saveConfig(FileManager.modulesConfig)
        }
    val hue = Math.random().toFloat()
    var slide = 0F
    fun getTagName(tagleft:String,tagright:String):String{
        return "$name${if (tag == null) "" else " §7$tagleft$tag$tagright"}"
    }
    fun getColorlessTagName(tagleft:String,tagright:String):String{
        return "$name${if (tag == null) "" else " $tagleft${stripColor(tag)}$tagright"}"
    }
    var slideStep = 0F
    var array = true
        set(array) {
            field = array
            if (!KevinClient.isStarting) FileManager.saveConfig(FileManager.modulesConfig)
        }
    open val tag: String?
        get() = null

    var autoDisable = false to ""

    fun toggle() {
        state = !state
    }

    open fun onEnable() {}

    open fun onDisable() {}

    open fun onToggle(state: Boolean){}

    override fun handleEvents() = state

    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass,this)
}