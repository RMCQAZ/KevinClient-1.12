package kevin.hud.element.elements

import kevin.font.AWTFontRenderer.Companion.assumeNonVolatile
import kevin.hud.element.Border
import kevin.hud.element.Element
import kevin.hud.element.ElementInfo
import kevin.hud.element.Side
import net.minecraft.client.resources.I18n
import net.minecraft.potion.Potion

@ElementInfo(name = "Effects")
class Effects(x: Double = 5.0, y: Double = 50.0, scale: Float = 1F,
              side: Side = Side(Side.Horizontal.LEFT, Side.Vertical.MIDDLE)) : Element(x, y, scale, side) {

    override fun drawElement(): Border {
        var y = 0F
        var width = 0F

        val fontRenderer = mc.fontRenderer

        assumeNonVolatile = true

        for (effect in mc.player!!.activePotionEffects) {
            val potion = effect.potion

            val number = when {
                effect.amplifier == 1 -> "II"
                effect.amplifier == 2 -> "III"
                effect.amplifier == 3 -> "IV"
                effect.amplifier == 4 -> "V"
                effect.amplifier == 5 -> "VI"
                effect.amplifier == 6 -> "VII"
                effect.amplifier == 7 -> "VIII"
                effect.amplifier == 8 -> "IX"
                effect.amplifier == 9 -> "X"
                effect.amplifier > 10 -> "X+"
                else -> "I"
            }

            val color = if (Potion.getPotionDurationString(effect,1F) == "**:**") "§9"
            else if (Potion.getPotionDurationString(effect,1F).split(":")[1].toInt()<10 && Potion.getPotionDurationString(effect,1F).split(":")[0].toInt() == 0) "§c"
            else if (Potion.getPotionDurationString(effect,1F).split(":")[0].toInt() == 0) "§e"
            else "§a"

            val name = "${I18n.format(potion.name)} $number §f: $color${Potion.getPotionDurationString(effect,1F)}"

            fontRenderer.drawString(name, width, y, potion.liquidColor, true)
            y += fontRenderer.FONT_HEIGHT
        }

        assumeNonVolatile = false

        if (width == 0F)
            width = 40F

        if (y == 0F)
            y = -10F

        return Border(2F, fontRenderer.FONT_HEIGHT.toFloat(), -width - 2F, y + fontRenderer.FONT_HEIGHT - 2F)
    }
}