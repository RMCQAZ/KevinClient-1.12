package kevin.via

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.MathHelper

class ViaButton(width: Int) : GuiButton(4514,width - 95,5,90, 20, "") {
    private var dragging = false
    private var sliderValue = 0F
    private val step = 1F/28.5F
    private var setValue = 0
    init {
        upDateValue()
    }
    private fun upDateValue(){
        val value = ProtocolCollection.getProtocolById(ViaVersion.nowVersion)!!
        displayString = value.protocolVersion.name
        setValue = ViaVersion.versions.indexOf(value)
        this.sliderValue = if (value!=ViaVersion.versions.last()) (setValue*step) else 1F
    }
    override fun mouseReleased(mouseX: Int, mouseY: Int) {
        dragging = false
        ViaVersion.nowVersion = ViaVersion.versions[setValue].protocolVersion.version
    }
    override fun getHoverState(mouseOver: Boolean): Int { return 0 }
    override fun mouseDragged(mc: Minecraft, mouseX: Int, mouseY: Int) {
        if (visible) {
            if (dragging) {
                this.sliderValue = (mouseX - (x + 4)).toFloat() / (width - 8).toFloat()
                this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0f, 1.0f)
                setValue = (sliderValue/step).toInt()
                val value = ViaVersion.versions[setValue]
                this.sliderValue = if (value!=ViaVersion.versions.last()) ((sliderValue/step).toInt()*step) else 1F
                displayString = value.protocolVersion.name
            }
            mc.textureManager.bindTexture(BUTTON_TEXTURES)
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            this.drawTexturedModalRect(
                x + (this.sliderValue * (width - 8).toFloat()).toInt(),
                y, 0, 66, 4, 20
            )
            this.drawTexturedModalRect(
                x + (this.sliderValue * (width - 8).toFloat()).toInt() + 4,
                y, 196, 66, 4, 20
            )
        }
    }
    override fun mousePressed(mc: Minecraft, mouseX: Int, mouseY: Int): Boolean {
        return if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (mouseX - (x + 4)).toFloat() / (width - 8).toFloat()
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0f, 1.0f)
            setValue = (sliderValue/step).toInt()
            val value = ViaVersion.versions[setValue]
            this.sliderValue = if (value!=ViaVersion.versions.last()) ((sliderValue/step).toInt()*step) else 1F
            displayString = value.protocolVersion.name
            dragging = true
            true
        } else {
            false
        }
    }
}